package dao;

import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomerDAO - Data Access Object for Customer entities.
 * Fully migrated to Neo4j Cypher queries using Neo4jConnection.
 * Preserves the exact public API for full compatibility with the Swing UI.
 */
public class CustomerDAO {

    /**
     * Retrieves all customer records from Neo4j.
     *
     * @return List of String arrays matching table columns:
     *         [Person ID, First Name, DOB, Email, Aadhar, Address]
     */
    public List<String[]> displayCustomers() {
        List<String[]> results = new ArrayList<>();

        String cypher =
            "MATCH (c:Customer) " +
            "RETURN coalesce(c.personId, c.Person_ID, '') AS personId, " +
            "       coalesce(c.firstName, c.F_Name, '') AS firstName, " +
            "       coalesce(toString(c.dob), toString(c.DOB), '') AS dob, " +
            "       coalesce(c.email, c.Email, '') AS email, " +
            "       coalesce(c.aadhar, c.Aadhar, '') AS aadhar, " +
            "       coalesce(c.address, c.Address, '') AS address " +
            "ORDER BY personId";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record r = result.next();
                results.add(new String[] {
                    r.get("personId", ""),
                    r.get("firstName", ""),
                    r.get("dob", ""),
                    r.get("email", ""),
                    r.get("aadhar", ""),
                    r.get("address", "")
                });
            }
            return results;
        } catch (Exception e) {
            System.err.println("Error retrieving customers from Neo4j: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adds a new Customer node to Neo4j.
     * Verifies that personId is unique before creating.
     *
     * @return true if created successfully, false if duplicate or failed
     */
    public boolean addCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        String checkCypher =
            "MATCH (c:Customer) " +
            "WHERE c.personId = $personId OR c.Person_ID = $personId " +
            "RETURN count(c) AS cnt";

        String createCypher =
            "CREATE (c:Customer { " +
            "    personId: $personId, " +
            "    firstName: $firstName, " +
            "    dob: $dob, " +
            "    email: $email, " +
            "    aadhar: $aadhar, " +
            "    address: $address " +
            "})";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result checkResult = tx.run(checkCypher, Values.parameters("personId", personId));
                if (checkResult.hasNext() && checkResult.next().get("cnt").asInt() > 0) {
                    return false;
                }

                tx.run(createCypher, Values.parameters(
                    "personId", personId,
                    "firstName", firstName,
                    "dob", dob,
                    "email", email,
                    "aadhar", aadhar,
                    "address", address
                ));
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds a single customer by person ID.
     *
     * @param personId unique customer identifier
     * @return String array of customer fields or null if not found
     */
    public String[] findCustomer(String personId) {
        String cypher =
            "MATCH (c:Customer) " +
            "WHERE c.personId = $personId OR c.Person_ID = $personId " +
            "RETURN coalesce(c.personId, c.Person_ID, '') AS personId, " +
            "       coalesce(c.firstName, c.F_Name, '') AS firstName, " +
            "       coalesce(toString(c.dob), toString(c.DOB), '') AS dob, " +
            "       coalesce(c.email, c.Email, '') AS email, " +
            "       coalesce(c.aadhar, c.Aadhar, '') AS aadhar, " +
            "       coalesce(c.address, c.Address, '') AS address";

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher, Values.parameters("personId", personId));
            if (result.hasNext()) {
                Record r = result.next();
                return new String[] {
                    r.get("personId", ""),
                    r.get("firstName", ""),
                    r.get("dob", ""),
                    r.get("email", ""),
                    r.get("aadhar", ""),
                    r.get("address", "")
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Updates an existing customer's fields.
     *
     * @return true if updated, false if not found or update failed
     */
    public boolean updateCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        String cypher =
            "MATCH (c:Customer) " +
            "WHERE c.personId = $personId OR c.Person_ID = $personId " +
            "SET c.personId = $personId, " +
            "    c.firstName = $firstName, " +
            "    c.dob = $dob, " +
            "    c.email = $email, " +
            "    c.aadhar = $aadhar, " +
            "    c.address = $address " +
            "RETURN count(c) AS updatedCount";

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Values.parameters(
                    "personId", personId,
                    "firstName", firstName,
                    "dob", dob,
                    "email", email,
                    "aadhar", aadhar,
                    "address", address
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
     * Deletes a customer by person ID using full cascade deletion.
     * Deletes exclusively owned policies, their exclusively dependent claims,
     * and their exclusively dependent payments.
     * Preserves covered assets, service providers, and shared policies.
     *
     * @param personId unique customer identifier
     * @return true if deleted, false if not found or deletion failed
     */
    public boolean deleteCustomer(String personId) {
        if (personId == null || personId.trim().isEmpty()) {
            return false;
        }

        try (Session session = Neo4jConnection.getSession()) {
            return session.executeWrite(tx -> {
                // 1. Verify Customer exists
                Result checkResult = tx.run(
                    "MATCH (c:Customer) " +
                    "WHERE c.personId = $personId OR c.Person_ID = $personId " +
                    "RETURN count(c) AS custCount",
                    Values.parameters("personId", personId)
                );

                if (!checkResult.hasNext() || checkResult.next().get("custCount").asInt() == 0) {
                    System.err.println("Customer '" + personId + "' does not exist.");
                    return false;
                }

                // 2. Full cascade deletion:
                // Delete exclusively dependent Payments, Claims, and Policies, then delete the Customer node.
                // Preserves covered Asset nodes, ServiceProvider nodes, and shared Policies.
                // Uses existential subqueries (EXISTS) and distinct collection aggregation to prevent duplicate deletion.
                String deleteCypher =
                    "MATCH (c:Customer) " +
                    "WHERE c.personId = $personId OR c.Person_ID = $personId " +
                    "OPTIONAL MATCH (c)-[rPol]-(p:Policy) " +
                    "WHERE NOT EXISTS { " +
                    "    MATCH (p)--(otherCust:Customer) " +
                    "    WHERE otherCust <> c " +
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
                    "WITH c, " +
                    "     collect(DISTINCT p)   AS policies, " +
                    "     collect(DISTINCT cl)  AS claims, " +
                    "     collect(DISTINCT pay) AS payments " +
                    "FOREACH (item IN payments | DETACH DELETE item) " +
                    "FOREACH (item IN claims   | DETACH DELETE item) " +
                    "FOREACH (item IN policies | DETACH DELETE item) " +
                    "DETACH DELETE c";

                Result result = tx.run(deleteCypher, Values.parameters("personId", personId));
                return result.consume().counters().nodesDeleted() > 0;
            });
        } catch (Exception e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the total number of Customer nodes in the graph.
     *
     * @return total customer count
     */
    public int getCustomerCount() {
        String cypher = "MATCH (c:Customer) RETURN count(c) AS cnt";

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
}
