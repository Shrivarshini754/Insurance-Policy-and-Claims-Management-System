package dao;

import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * AssetDAO - Data Access Object for Insurance Asset entities.
 * Fully migrated to Neo4j Cypher queries using Neo4jConnection.
 * Preserves all existing method signatures and parameter ordering for
 * seamless integration with MainFrame, BackendService, and AssetPanel.
 */
public class AssetDAO {

    /**
     * Retrieves all assets from Neo4j.
     *
     * @return List of String arrays matching table columns:
     *         [Asset ID, Description, Purchase Date, Estimated Value]
     */
    public List<String[]> displayAssets() {
        List<String[]> results = new ArrayList<>();

        String cypher =
            "MATCH (a:Asset) " +
            "RETURN coalesce(a.assetId, a.Asset_ID, '') AS assetId, " +
            "       coalesce(a.description, a.Descr, '') AS description, " +
            "       coalesce(toString(a.purchaseDate), toString(a.P_Date), '') AS purchaseDate, " +
            "       coalesce(a.estimatedValue, a.Est_Value, 0.0) AS estimatedValue " +
            "ORDER BY assetId";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("assetId", ""),
                    r.get("description", ""),
                    r.get("purchaseDate", ""),
                    String.valueOf(r.get("estimatedValue").asDouble(0.0))
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Adds a new Asset node to Neo4j.
     * Verifies assetId is unique before creation.
     *
     * @param assetId        unique asset identifier
     * @param description    asset description
     * @param purchaseDate   purchase date in YYYY-MM-DD format
     * @param estimatedValue estimated monetary valuation
     * @return true if added successfully, false if duplicate or failed
     */
    public boolean addAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        String checkCypher =
            "MATCH (a:Asset) " +
            "WHERE a.assetId = $assetId OR a.Asset_ID = $assetId " +
            "RETURN count(a) AS cnt";

        String createCypher =
            "CREATE (a:Asset { " +
            "    assetId: $assetId, " +
            "    description: $description, " +
            "    purchaseDate: $purchaseDate, " +
            "    estimatedValue: $estimatedValue " +
            "})";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result checkResult = tx.run(checkCypher, Values.parameters("assetId", assetId));
                if (checkResult.hasNext() && checkResult.next().get("cnt").asInt() > 0) {
                    return false;
                }

                tx.run(createCypher, Values.parameters(
                    "assetId", assetId,
                    "description", description,
                    "purchaseDate", purchaseDate,
                    "estimatedValue", estimatedValue
                ));
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing asset's fields in Neo4j.
     *
     * @param assetId        unique asset identifier
     * @param description    updated description
     * @param purchaseDate   updated purchase date
     * @param estimatedValue updated estimated value
     * @return true if updated, false if not found or failed
     */
    public boolean updateAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        String cypher =
            "MATCH (a:Asset) " +
            "WHERE a.assetId = $assetId OR a.Asset_ID = $assetId " +
            "SET a.assetId = $assetId, " +
            "    a.description = $description, " +
            "    a.purchaseDate = $purchaseDate, " +
            "    a.estimatedValue = $estimatedValue " +
            "RETURN count(a) AS updatedCount";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Values.parameters(
                    "assetId", assetId,
                    "description", description,
                    "purchaseDate", purchaseDate,
                    "estimatedValue", estimatedValue
                ));
                return result.hasNext() && result.next().get("updatedCount").asInt() > 0;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes an asset by asset ID using full cascade deletion.
     * Deletes the asset and any orphan policies exclusively covering this asset
     * that have no customer owner, along with their exclusively dependent claims and payments.
     * Preserves customer-owned policies, shared policies, and service providers.
     *
     * @param assetId unique asset identifier
     * @return true if deleted, false if not found or deletion failed
     */
    public boolean deleteAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify Asset exists
                Result checkResult = tx.run(
                    "MATCH (a:Asset) " +
                    "WHERE a.assetId = $assetId OR a.Asset_ID = $assetId " +
                    "RETURN count(a) AS cnt",
                    Values.parameters("assetId", assetId)
                );

                if (!checkResult.hasNext() || checkResult.next().get("cnt").asInt() == 0) {
                    System.err.println("Asset '" + assetId + "' does not exist.");
                    return false;
                }

                // 2. Cascade delete:
                // Delete orphan policies exclusively covering this asset (with no customer owner),
                // their exclusive claims/payments, and the Asset node.
                // Preserves customer-owned policies, multi-asset policies, and service providers.
                // Uses existential subqueries (EXISTS) and distinct collection aggregation to prevent duplicate deletion.
                String deleteCypher =
                    "MATCH (a:Asset) " +
                    "WHERE a.assetId = $assetId OR a.Asset_ID = $assetId " +
                    "OPTIONAL MATCH (a)-[rPol]-(p:Policy) " +
                    "WHERE NOT EXISTS { " +
                    "    MATCH (p)--(otherAsset:Asset) " +
                    "    WHERE otherAsset <> a " +
                    "} " +
                    "  AND NOT EXISTS { " +
                    "    MATCH (p)--(:Customer) " +
                    "} " +
                    "OPTIONAL MATCH (p)-[rCl]-(cl:Claim) " +
                    "WHERE NOT EXISTS { " +
                    "    MATCH (cl)--(otherPol:Policy) " +
                    "    WHERE otherPol <> p " +
                    "} " +
                    "OPTIONAL MATCH (cl)-[rPay]-(pay:Payment) " +
                    "WHERE NOT EXISTS { " +
                    "    MATCH (pay)--(otherCl:Claim) " +
                    "    WHERE otherCl <> cl " +
                    "} " +
                    "WITH a, " +
                    "     collect(DISTINCT p)   AS policies, " +
                    "     collect(DISTINCT cl)  AS claims, " +
                    "     collect(DISTINCT pay) AS payments " +
                    "FOREACH (item IN payments | DETACH DELETE item) " +
                    "FOREACH (item IN claims   | DETACH DELETE item) " +
                    "FOREACH (item IN policies | DETACH DELETE item) " +
                    "DETACH DELETE a";

                Result result = tx.run(deleteCypher, Values.parameters("assetId", assetId));
                return result.consume().counters().nodesDeleted() > 0;
            });
        } catch (Exception e) {
            System.err.println("Error deleting asset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the total number of Asset nodes in Neo4j.
     *
     * @return total asset count
     */
    public int getAssetCount() {
        String cypher = "MATCH (a:Asset) RETURN count(a) AS cnt";

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
     * Finds a single asset by asset ID.
     *
     * @param assetId unique asset identifier
     * @return String array of asset fields or null if not found
     */
    public String[] findAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return null;
        }

        String cypher =
            "MATCH (a:Asset) " +
            "WHERE a.assetId = $assetId OR a.Asset_ID = $assetId " +
            "RETURN coalesce(a.assetId, a.Asset_ID, '') AS assetId, " +
            "       coalesce(a.description, a.Descr, '') AS description, " +
            "       coalesce(toString(a.purchaseDate), toString(a.P_Date), '') AS purchaseDate, " +
            "       coalesce(a.estimatedValue, a.Est_Value, 0.0) AS estimatedValue";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("assetId", assetId));
            if (result.hasNext()) {
                Record r = result.next();
                return new String[] {
                    r.get("assetId", ""),
                    r.get("description", ""),
                    r.get("purchaseDate", ""),
                    String.valueOf(r.get("estimatedValue").asDouble(0.0))
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
