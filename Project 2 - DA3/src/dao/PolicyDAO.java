package dao;

import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * PolicyDAO - Data Access Object for Insurance Policy entities.
 * Fully migrated to Neo4j Cypher queries using Neo4jConnection.
 * Preserves all existing method signatures and parameter ordering for
 * seamless integration with MainFrame, BackendService, and PolicyPanel.
 */
public class PolicyDAO {

    /**
     * Retrieves all policies from Neo4j.
     *
     * @return List of String arrays matching table columns:
     *         [Policy No., Start Date, Expiry Date, Audit ID, Premium Amount]
     */
    public List<String[]> displayPolicies() {
        List<String[]> results = new ArrayList<>();

        String cypher =
            "MATCH (p:Policy) " +
            "RETURN coalesce(p.policyNo, p.P_No, '') AS policyNo, " +
            "       coalesce(toString(p.startDate), toString(p.Start_Date), '') AS startDate, " +
            "       coalesce(toString(p.expiryDate), toString(p.Expiry_Date), '') AS expiryDate, " +
            "       coalesce(p.auditId, p.Audit_ID, '') AS auditId, " +
            "       coalesce(p.premiumAmount, p.Premium_Amt, 0.0) AS premiumAmount " +
            "ORDER BY policyNo";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("policyNo", ""),
                    r.get("startDate", ""),
                    r.get("expiryDate", ""),
                    r.get("auditId", ""),
                    String.valueOf(r.get("premiumAmount").asDouble(0.0))
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Finds a single policy by policy number.
     *
     * @param policyNo unique policy identifier (P_No)
     * @return String array of policy fields or null if not found
     */
    public String[] findPolicy(String policyNo) {
        String cypher =
            "MATCH (p:Policy) " +
            "WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
            "RETURN coalesce(p.policyNo, p.P_No, '') AS policyNo, " +
            "       coalesce(toString(p.startDate), toString(p.Start_Date), '') AS startDate, " +
            "       coalesce(toString(p.expiryDate), toString(p.Expiry_Date), '') AS expiryDate, " +
            "       coalesce(p.auditId, p.Audit_ID, '') AS auditId, " +
            "       coalesce(p.premiumAmount, p.Premium_Amt, 0.0) AS premiumAmount";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("policyNo", policyNo));
            if (result.hasNext()) {
                Record r = result.next();
                return new String[] {
                    r.get("policyNo", ""),
                    r.get("startDate", ""),
                    r.get("expiryDate", ""),
                    r.get("auditId", ""),
                    String.valueOf(r.get("premiumAmount").asDouble(0.0))
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Adds a new Policy node to Neo4j.
     * Verifies that policyNo is unique before creating.
     *
     * @return true if added successfully, false if duplicate or failed
     */
    public boolean addPolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        String checkCypher =
            "MATCH (p:Policy) " +
            "WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
            "RETURN count(p) AS cnt";

        String createCypher =
            "CREATE (p:Policy { " +
            "    policyNo: $policyNo, " +
            "    startDate: $startDate, " +
            "    expiryDate: $expiryDate, " +
            "    auditId: $auditId, " +
            "    premiumAmount: $premiumAmount " +
            "})";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result checkResult = tx.run(checkCypher, Values.parameters("policyNo", policyNo));
                if (checkResult.hasNext() && checkResult.next().get("cnt").asInt() > 0) {
                    return false;
                }

                tx.run(createCypher, Values.parameters(
                    "policyNo", policyNo,
                    "startDate", startDate,
                    "expiryDate", expiryDate,
                    "auditId", auditId,
                    "premiumAmount", premiumAmount
                ));
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing policy's fields in Neo4j.
     *
     * @return true if updated, false if policy not found or update failed
     */
    public boolean updatePolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        String cypher =
            "MATCH (p:Policy) " +
            "WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
            "SET p.policyNo = $policyNo, " +
            "    p.startDate = $startDate, " +
            "    p.expiryDate = $expiryDate, " +
            "    p.auditId = $auditId, " +
            "    p.premiumAmount = $premiumAmount " +
            "RETURN count(p) AS updatedCount";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Values.parameters(
                    "policyNo", policyNo,
                    "startDate", startDate,
                    "expiryDate", expiryDate,
                    "auditId", auditId,
                    "premiumAmount", premiumAmount
                ));
                if (result.hasNext()) {
                    return result.next().get("updatedCount").asInt() > 0;
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a policy by policy number if no relationships exist.
     * Prevents orphan-causing deletions if referenced by claims.
     *
     * @param policyNo unique policy identifier
     * @return true if deleted, false if not found or if relationships exist
     */
    public boolean deletePolicy(String policyNo) {
        String cypher =
            "MATCH (p:Policy) " +
            "WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
            "OPTIONAL MATCH (p)-[r]-() " +
            "WITH p, count(r) AS relCount " +
            "WHERE relCount = 0 " +
            "DELETE p " +
            "RETURN count(p) AS deletedCount";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Values.parameters("policyNo", policyNo));
                if (result.hasNext()) {
                    return result.next().get("deletedCount").asInt() > 0;
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the total number of Policy nodes in Neo4j.
     *
     * @return total policy count
     */
    public int getPolicyCount() {
        String cypher = "MATCH (p:Policy) RETURN count(p) AS cnt";

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
     * Counts policies whose expiry date is on or after the current date.
     * Uses ISO-8601 string comparison with the current date.
     *
     * @return active policy count
     */
    public int getActivePolicyCount() {
        String cypher =
            "MATCH (p:Policy) " +
            "WHERE coalesce(toString(p.expiryDate), toString(p.Expiry_Date), '') >= toString(date()) " +
            "RETURN count(p) AS cnt";

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
     * Calculates the sum of all policy premiums.
     *
     * @return total premium amount
     */
    public double getTotalPremium() {
        String cypher =
            "MATCH (p:Policy) " +
            "RETURN coalesce(sum(coalesce(p.premiumAmount, p.Premium_Amt, 0.0)), 0.0) AS total";

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
}
