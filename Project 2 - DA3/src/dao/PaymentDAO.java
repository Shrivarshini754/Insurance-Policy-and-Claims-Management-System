package dao;

import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentDAO - Data Access Object for Payment entities.
 * Fully migrated to Neo4j Cypher queries using Neo4jConnection.
 * Preserves all public method signatures, composite key handling,
 * and (:Claim)-[:HAS_PAYMENT]->(:Payment) relationship linking.
 */
public class PaymentDAO {

    /**
     * Retrieves all payment records from Neo4j.
     *
     * @return List of String arrays matching table columns:
     *         [Claim ID, Payment ID, Payment Date, Payment Method, Amount Paid]
     */
    public List<String[]> displayPayments() {
        List<String[]> results = new ArrayList<>();

        String cypher =
            "MATCH (pay:Payment) " +
            "OPTIONAL MATCH (c:Claim)-[:HAS_PAYMENT]->(pay) " +
            "RETURN coalesce(pay.claimId, c.claimId, pay.Claim_ID, c.Claim_ID, '') AS claimId, " +
            "       coalesce(pay.paymentId, pay.Payment_ID, '') AS paymentId, " +
            "       coalesce(toString(pay.paymentDate), toString(pay.P_Date), '') AS paymentDate, " +
            "       coalesce(pay.paymentMethod, pay.P_Method, '') AS paymentMethod, " +
            "       coalesce(pay.amountPaid, pay.Amt_Paid, 0.0) AS amountPaid " +
            "ORDER BY claimId, paymentId";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("claimId", ""),
                    r.get("paymentId", ""),
                    r.get("paymentDate", ""),
                    r.get("paymentMethod", ""),
                    String.valueOf(r.get("amountPaid").asDouble(0.0))
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Records a new payment in Neo4j and links it to the referenced Claim node
     * via (:Claim)-[:HAS_PAYMENT]->(:Payment).
     *
     * Enforces:
     *  1. Composite primary key uniqueness (claimId + paymentId).
     *  2. Claim existence (rejects creation if referenced claim does not exist).
     *
     * @param claimId   associated claim identifier
     * @param paymentId unique payment identifier within the claim
     * @param date      payment date in YYYY-MM-DD format
     * @param method    payment method (e.g. Bank Transfer, Online, Cheque, Cash)
     * @return true if recorded successfully, false if duplicate or claim missing
     */
    public boolean makePayment(
            String claimId,
            String paymentId,
            String date,
            String method) {

        return makePayment(claimId, paymentId, date, method, 0.0);
    }

    /**
     * Overloaded method to record a payment with an explicit amount.
     *
     * @param claimId    associated claim identifier
     * @param paymentId  unique payment identifier within the claim
     * @param date       payment date in YYYY-MM-DD format
     * @param method     payment method
     * @param amountPaid monetary payment amount
     * @return true if recorded successfully, false otherwise
     */
    public boolean makePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            System.err.println("Claim ID and Payment ID are required.");
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify composite primary key (claimId + paymentId) does not exist
                Result checkPayment = tx.run(
                    "MATCH (pay:Payment) " +
                    "WHERE (pay.claimId = $claimId OR pay.Claim_ID = $claimId) " +
                    "  AND (pay.paymentId = $paymentId OR pay.Payment_ID = $paymentId) " +
                    "RETURN count(pay) AS cnt",
                    Values.parameters("claimId", claimId, "paymentId", paymentId)
                );
                if (checkPayment.hasNext() && checkPayment.next().get("cnt").asInt() > 0) {
                    System.err.println("Payment with ID '" + paymentId + "' for Claim '" + claimId + "' already exists.");
                    return false;
                }

                // 2. Verify referenced Claim exists
                Result checkClaim = tx.run(
                    "MATCH (c:Claim) WHERE c.claimId = $claimId OR c.Claim_ID = $claimId RETURN count(c) AS cnt",
                    Values.parameters("claimId", claimId)
                );
                if (!checkClaim.hasNext() || checkClaim.next().get("cnt").asInt() == 0) {
                    System.err.println("Referenced claim '" + claimId + "' does not exist. Cannot record payment.");
                    return false;
                }

                // 3. Create Payment node and (:Claim)-[:HAS_PAYMENT]->(:Payment) relationship
                String createCypher =
                    "MATCH (c:Claim) WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "CREATE (pay:Payment { " +
                    "    claimId: $claimId, " +
                    "    paymentId: $paymentId, " +
                    "    paymentDate: $date, " +
                    "    paymentMethod: $method, " +
                    "    amountPaid: $amountPaid " +
                    "}) " +
                    "CREATE (c)-[:HAS_PAYMENT]->(pay)";

                tx.run(createCypher, Values.parameters(
                    "claimId", claimId,
                    "paymentId", paymentId,
                    "date", date,
                    "method", method,
                    "amountPaid", amountPaid
                ));

                return true;
            });
        } catch (Exception e) {
            System.err.println("Error recording payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing payment's fields in Neo4j.
     *
     * @param claimId    associated claim identifier
     * @param paymentId  payment identifier
     * @param date       updated payment date
     * @param method     updated payment method
     * @param amountPaid updated monetary amount
     * @return true if updated successfully, false if payment or claim not found
     */
    public boolean updatePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify referenced Claim exists
                Result checkClaim = tx.run(
                    "MATCH (c:Claim) WHERE c.claimId = $claimId OR c.Claim_ID = $claimId RETURN count(c) AS cnt",
                    Values.parameters("claimId", claimId)
                );
                if (!checkClaim.hasNext() || checkClaim.next().get("cnt").asInt() == 0) {
                    System.err.println("Referenced claim '" + claimId + "' does not exist. Cannot update payment.");
                    return false;
                }

                // 2. Update Payment properties and maintain relationship
                String updateCypher =
                    "MATCH (pay:Payment) " +
                    "WHERE (pay.claimId = $claimId OR pay.Claim_ID = $claimId) " +
                    "  AND (pay.paymentId = $paymentId OR pay.Payment_ID = $paymentId) " +
                    "MATCH (c:Claim) " +
                    "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "SET pay.paymentDate = $date, " +
                    "    pay.paymentMethod = $method, " +
                    "    pay.amountPaid = $amountPaid " +
                    "WITH pay, c " +
                    "OPTIONAL MATCH (otherClaim:Claim)-[oldRel:HAS_PAYMENT]->(pay) " +
                    "WHERE otherClaim <> c " +
                    "DELETE oldRel " +
                    "MERGE (c)-[:HAS_PAYMENT]->(pay) " +
                    "RETURN count(pay) AS cnt";

                Result result = tx.run(updateCypher, Values.parameters(
                    "claimId", claimId,
                    "paymentId", paymentId,
                    "date", date,
                    "method", method,
                    "amountPaid", amountPaid
                ));

                return result.hasNext() && result.next().get("cnt").asInt() > 0;
            });
        } catch (Exception e) {
            System.err.println("Error updating payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a payment record by claim ID and payment ID.
     *
     * @param claimId   associated claim identifier
     * @param paymentId payment identifier
     * @return true if deleted, false if not found
     */
    public boolean deletePayment(String claimId, String paymentId) {
        String cypher =
            "MATCH (pay:Payment) " +
            "WHERE (pay.claimId = $claimId OR pay.Claim_ID = $claimId) " +
            "  AND (pay.paymentId = $paymentId OR pay.Payment_ID = $paymentId) " +
            "DETACH DELETE pay " +
            "RETURN count(pay) AS deletedCount";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Values.parameters(
                    "claimId", claimId,
                    "paymentId", paymentId
                ));
                return result.hasNext() && result.next().get("deletedCount").asInt() > 0;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the total number of Payment nodes in Neo4j.
     *
     * @return total payment count
     */
    public int getPaymentCount() {
        String cypher = "MATCH (pay:Payment) RETURN count(pay) AS cnt";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            if (result.hasNext()) {
                return result.next().get("cnt").asInt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Calculates the sum of all payments made.
     * Replaces Oracle SQL: SELECT NVL(SUM(Amt_Paid), 0) FROM Payment2
     *
     * @return total amount paid
     */
    public double getTotalAmountPaid() {
        String cypher =
            "MATCH (pay:Payment) " +
            "RETURN coalesce(sum(coalesce(pay.amountPaid, pay.Amt_Paid, 0.0)), 0.0) AS total";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            if (result.hasNext()) {
                return result.next().get("total").asDouble(0.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    /**
     * Finds a single payment by claim ID and payment ID.
     *
     * @param claimId   claim identifier
     * @param paymentId payment identifier
     * @return String array of payment fields or null if not found
     */
    public String[] findPayment(String claimId, String paymentId) {
        String cypher =
            "MATCH (pay:Payment) " +
            "WHERE (pay.claimId = $claimId OR pay.Claim_ID = $claimId) " +
            "  AND (pay.paymentId = $paymentId OR pay.Payment_ID = $paymentId) " +
            "OPTIONAL MATCH (c:Claim)-[:HAS_PAYMENT]->(pay) " +
            "RETURN coalesce(pay.claimId, c.claimId, pay.Claim_ID, c.Claim_ID, '') AS claimId, " +
            "       coalesce(pay.paymentId, pay.Payment_ID, '') AS paymentId, " +
            "       coalesce(toString(pay.paymentDate), toString(pay.P_Date), '') AS paymentDate, " +
            "       coalesce(pay.paymentMethod, pay.P_Method, '') AS paymentMethod, " +
            "       coalesce(pay.amountPaid, pay.Amt_Paid, 0.0) AS amountPaid";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters(
                "claimId", claimId,
                "paymentId", paymentId
            ));
            if (result.hasNext()) {
                Record r = result.next();
                return new String[] {
                    r.get("claimId", ""),
                    r.get("paymentId", ""),
                    r.get("paymentDate", ""),
                    r.get("paymentMethod", ""),
                    String.valueOf(r.get("amountPaid").asDouble(0.0))
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
