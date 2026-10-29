package dao;

import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * ClaimDAO - Data Access Object for Insurance Claim entities.
 * Fully migrated to Neo4j Cypher queries using Neo4jConnection.
 * Preserves all public method signatures for compatibility with
 * BackendService, MainFrame, ClaimPanel, and TestBackend.
 */
public class ClaimDAO {

    /**
     * Retrieves all claim records from Neo4j.
     *
     * @return List of String arrays matching table columns:
     *         [Claim ID, Policy No., Incident Date, Description, Claimed Amount, Status]
     */
    public List<String[]> displayClaims() {
        List<String[]> results = new ArrayList<>();

        String cypher =
            "MATCH (c:Claim) " +
            "OPTIONAL MATCH (p:Policy)-[:HAS_CLAIM]->(c) " +
            "RETURN coalesce(c.claimId, c.Claim_ID, '') AS claimId, " +
            "       coalesce(c.policyNo, p.policyNo, c.P_No, p.P_No, '') AS policyNo, " +
            "       coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') AS incidentDate, " +
            "       coalesce(c.description, c.Descr, '') AS description, " +
            "       coalesce(c.claimedAmount, c.Claimed_Amt, 0.0) AS claimedAmount, " +
            "       coalesce(c.status, c.Claim_Status, '') AS status " +
            "ORDER BY claimId";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("claimId", ""),
                    r.get("policyNo", ""),
                    r.get("incidentDate", ""),
                    r.get("description", ""),
                    String.valueOf(r.get("claimedAmount").asDouble(0.0)),
                    r.get("status", "")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Registers a new claim in Neo4j and links it to the referenced Policy node
     * via (:Policy)-[:HAS_CLAIM]->(:Claim).
     *
     * Enforces:
     *  1. Claim ID uniqueness.
     *  2. Policy existence (rejects creation if referenced policy does not exist).
     *  3. Policy expiry check (rejects if the policy has already expired).
     *
     * @param claimId      unique claim identifier
     * @param policyNo     associated policy number
     * @param incidentDate incident date in YYYY-MM-DD format
     * @param description  claim description
     * @param status       initial claim status (e.g. Pending, Approved, Rejected)
     * @return true if registered successfully, false otherwise
     */
    public boolean registerClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            String status) {

        if (claimId == null || claimId.trim().isEmpty() ||
            policyNo == null || policyNo.trim().isEmpty()) {
            System.err.println("Claim ID and Policy Number are required.");
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify claim ID does not already exist
                Result checkClaim = tx.run(
                    "MATCH (c:Claim) " +
                    "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "RETURN count(c) AS cnt",
                    Values.parameters("claimId", claimId)
                );
                if (checkClaim.hasNext() && checkClaim.next().get("cnt").asInt() > 0) {
                    System.err.println("Claim with ID '" + claimId + "' already exists.");
                    return false;
                }

                // 2. Verify referenced Policy exists
                Result checkPolicy = tx.run(
                    "MATCH (p:Policy) WHERE p.policyNo = $policyNo OR p.P_No = $policyNo RETURN count(p) AS cnt",
                    Values.parameters("policyNo", policyNo)
                );
                if (!checkPolicy.hasNext() || checkPolicy.next().get("cnt").asInt() == 0) {
                    System.err.println("Referenced policy '" + policyNo + "' does not exist. Cannot register claim.");
                    return false;
                }

                // 3. Check policy expiry
                Result expiryResult = tx.run(
                    "MATCH (p:Policy) WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
                    "RETURN coalesce(toString(p.expiryDate), toString(p.Expiry_Date), '') AS expiryDate",
                    Values.parameters("policyNo", policyNo)
                );
                if (expiryResult.hasNext()) {
                    String expiryDate = expiryResult.next().get("expiryDate", "");
                    if (!expiryDate.isEmpty()) {
                        String today = java.time.LocalDate.now().toString();
                        if (expiryDate.compareTo(today) < 0) {
                            System.err.println("Policy '" + policyNo + "' has expired on " + expiryDate + ". Cannot register claim.");
                            return false;
                        }
                    }
                }

                // 4. Create Claim node and (:Policy)-[:HAS_CLAIM]->(:Claim) relationship
                String createCypher =
                    "MATCH (p:Policy) WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
                    "CREATE (c:Claim { " +
                    "    claimId: $claimId, " +
                    "    policyNo: $policyNo, " +
                    "    incidentDate: $incidentDate, " +
                    "    description: $description, " +
                    "    claimedAmount: 0.0, " +
                    "    status: $status " +
                    "}) " +
                    "CREATE (p)-[:HAS_CLAIM]->(c)";

                tx.run(createCypher, Values.parameters(
                    "claimId", claimId,
                    "policyNo", policyNo,
                    "incidentDate", incidentDate,
                    "description", description,
                    "status", status
                ));

                return true;
            });
        } catch (Exception e) {
            System.err.println("Error registering claim: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the claimed amount for a specific claim.
     *
     * @param claimId unique claim identifier
     * @return the claimed amount, or 0.0 if not found
     */
    public double getClaimAmount(String claimId) {
        String cypher =
            "MATCH (c:Claim) " +
            "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
            "RETURN coalesce(c.claimedAmount, c.Claimed_Amt, 0.0) AS amount";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("claimId", claimId));
            if (result.hasNext()) {
                return result.next().get("amount").asDouble(0.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    /**
     * Updates an existing claim's fields in Neo4j.
     * Maintains the (:Policy)-[:HAS_CLAIM]->(:Claim) relationship
     * if the policy number changes.
     *
     * @param claimId       claim identifier (lookup key, not editable)
     * @param policyNo      associated policy number
     * @param incidentDate  incident date in YYYY-MM-DD format
     * @param description   updated description
     * @param claimedAmount updated claimed monetary amount
     * @param status        updated claim status
     * @return true if updated successfully, false if claim or policy not found
     */
    public boolean updateClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            double claimedAmount,
            String status) {

        if (claimId == null || claimId.trim().isEmpty()) {
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify referenced Policy exists
                Result checkPolicy = tx.run(
                    "MATCH (p:Policy) WHERE p.policyNo = $policyNo OR p.P_No = $policyNo RETURN count(p) AS cnt",
                    Values.parameters("policyNo", policyNo)
                );
                if (!checkPolicy.hasNext() || checkPolicy.next().get("cnt").asInt() == 0) {
                    System.err.println("Referenced policy '" + policyNo + "' does not exist. Cannot update claim.");
                    return false;
                }

                // 2. Update Claim properties and maintain relationship
                String updateCypher =
                    "MATCH (c:Claim) " +
                    "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "SET c.policyNo = $policyNo, " +
                    "    c.incidentDate = $incidentDate, " +
                    "    c.description = $description, " +
                    "    c.claimedAmount = $claimedAmount, " +
                    "    c.status = $status " +
                    "WITH c " +
                    "MATCH (p:Policy) " +
                    "WHERE p.policyNo = $policyNo OR p.P_No = $policyNo " +
                    "OPTIONAL MATCH (otherPolicy:Policy)-[oldRel:HAS_CLAIM]->(c) " +
                    "WHERE otherPolicy <> p " +
                    "DELETE oldRel " +
                    "MERGE (p)-[:HAS_CLAIM]->(c) " +
                    "RETURN count(c) AS cnt";

                Result result = tx.run(updateCypher, Values.parameters(
                    "claimId", claimId,
                    "policyNo", policyNo,
                    "incidentDate", incidentDate,
                    "description", description,
                    "claimedAmount", claimedAmount,
                    "status", status
                ));

                return result.hasNext() && result.next().get("cnt").asInt() > 0;
            });
        } catch (Exception e) {
            System.err.println("Error updating claim: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a claim by claim ID using full cascade deletion.
     * Deletes exclusively dependent payments.
     * Preserves associated Policy, ServiceProvider, Asset, and Customer nodes.
     *
     * @param claimId unique claim identifier
     * @return true if deleted, false if not found or deletion failed
     */
    public boolean deleteClaim(String claimId) {
        if (claimId == null || claimId.trim().isEmpty()) {
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify Claim exists
                Result checkResult = tx.run(
                    "MATCH (c:Claim) " +
                    "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "RETURN count(c) AS cnt",
                    Values.parameters("claimId", claimId)
                );

                if (!checkResult.hasNext() || checkResult.next().get("cnt").asInt() == 0) {
                    System.err.println("Claim '" + claimId + "' does not exist.");
                    return false;
                }

                // 2. Cascade delete:
                // Delete exclusively dependent Payments, then delete the Claim node.
                // Preserves associated Policy, ServiceProvider, Asset, and Customer nodes.
                // Uses existential subqueries (EXISTS) and distinct collection aggregation to prevent duplicate deletion.
                String deleteCypher =
                    "MATCH (c:Claim) " +
                    "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
                    "OPTIONAL MATCH (c)-[rPay]-(pay:Payment) " +
                    "WHERE NOT EXISTS { " +
                    "    MATCH (pay)--(otherClaim:Claim) " +
                    "    WHERE otherClaim <> c " +
                    "} " +
                    "WITH c, collect(DISTINCT pay) AS payments " +
                    "FOREACH (item IN payments | DETACH DELETE item) " +
                    "DETACH DELETE c";

                Result result = tx.run(deleteCypher, Values.parameters("claimId", claimId));
                return result.consume().counters().nodesDeleted() > 0;
            });
        } catch (Exception e) {
            System.err.println("Error deleting claim: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the total number of Claim nodes in Neo4j.
     *
     * @return total claim count
     */
    public int getClaimCount() {
        String cypher = "MATCH (c:Claim) RETURN count(c) AS cnt";

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
     * Counts claims whose incident date falls within the current year.
     * Uses ISO-8601 string comparison with year boundaries.
     *
     * @return count of claims this year
     */
    public int getClaimsThisYearCount() {
        String cypher =
            "MATCH (c:Claim) " +
            "WHERE coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') >= toString(date().year) + '-01-01' " +
            "  AND coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') <= toString(date().year) + '-12-31' " +
            "RETURN count(c) AS cnt";

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
     * Calculates the sum of all claimed amounts.
     *
     * @return total claimed amount
     */
    public double getTotalClaimedAmount() {
        String cypher =
            "MATCH (c:Claim) " +
            "RETURN coalesce(sum(coalesce(c.claimedAmount, c.Claimed_Amt, 0.0)), 0.0) AS total";

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
     * Retrieves the most recent claims ordered by incident date descending.
     *
     * @param limit maximum number of records to return
     * @return List of recent claim records
     */
    public List<String[]> getRecentClaims(int limit) {
        List<String[]> results = new ArrayList<>();

        if (limit <= 0) {
            return results;
        }

        String cypher =
            "MATCH (c:Claim) " +
            "OPTIONAL MATCH (p:Policy)-[:HAS_CLAIM]->(c) " +
            "RETURN coalesce(c.claimId, c.Claim_ID, '') AS claimId, " +
            "       coalesce(c.policyNo, p.policyNo, c.P_No, p.P_No, '') AS policyNo, " +
            "       coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') AS incidentDate, " +
            "       coalesce(c.description, c.Descr, '') AS description, " +
            "       coalesce(c.claimedAmount, c.Claimed_Amt, 0.0) AS claimedAmount, " +
            "       coalesce(c.status, c.Claim_Status, '') AS status " +
            "ORDER BY incidentDate DESC " +
            "LIMIT $limit";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("limit", limit));
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("claimId", ""),
                    r.get("policyNo", ""),
                    r.get("incidentDate", ""),
                    r.get("description", ""),
                    String.valueOf(r.get("claimedAmount").asDouble(0.0)),
                    r.get("status", "")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Finds a single claim by claim ID.
     *
     * @param claimId unique claim identifier
     * @return String array of claim fields or null if not found
     */
    public String[] findClaim(String claimId) {
        String cypher =
            "MATCH (c:Claim) " +
            "WHERE c.claimId = $claimId OR c.Claim_ID = $claimId " +
            "OPTIONAL MATCH (p:Policy)-[:HAS_CLAIM]->(c) " +
            "RETURN coalesce(c.claimId, c.Claim_ID, '') AS claimId, " +
            "       coalesce(c.policyNo, p.policyNo, c.P_No, p.P_No, '') AS policyNo, " +
            "       coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') AS incidentDate, " +
            "       coalesce(c.description, c.Descr, '') AS description, " +
            "       coalesce(c.claimedAmount, c.Claimed_Amt, 0.0) AS claimedAmount, " +
            "       coalesce(c.status, c.Claim_Status, '') AS status";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("claimId", claimId));
            if (result.hasNext()) {
                Record r = result.next();
                return new String[] {
                    r.get("claimId", ""),
                    r.get("policyNo", ""),
                    r.get("incidentDate", ""),
                    r.get("description", ""),
                    String.valueOf(r.get("claimedAmount").asDouble(0.0)),
                    r.get("status", "")
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
