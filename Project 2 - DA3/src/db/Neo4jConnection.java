package db;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * Neo4j connection manager for the Insurance Management System DA3.
 * Manages the lifecycle of the Neo4j Driver and provides sessions
 * configured for the 'insurance' database.
 */
public class Neo4jConnection {

    public static final String URI = "neo4j://127.0.0.1:7687";
    public static final String DATABASE = "insurance";
    public static final String USERNAME = "neo4j";
    public static final String PASSWORD_ENV_VAR = "NEO4J_PASSWORD";

    private static volatile Driver driver;

    private Neo4jConnection() {
        // Utility class; prevent instantiation
    }

    /**
     * Obtains the active Neo4j Driver instance, initializing it lazily if needed.
     * Reads the password from the NEO4J_PASSWORD environment variable.
     *
     * @return active Neo4j Driver instance
     * @throws IllegalStateException if NEO4J_PASSWORD is not set or empty
     */
    public static Driver getDriver() {
        if (driver == null) {
            synchronized (Neo4jConnection.class) {
                if (driver == null) {
                    String password = System.getenv(PASSWORD_ENV_VAR);
                    if (password == null || password.trim().isEmpty()) {
                        throw new IllegalStateException(
                            "Environment variable '" + PASSWORD_ENV_VAR + "' is not set. " +
                            "Please configure '" + PASSWORD_ENV_VAR + "' before connecting to Neo4j."
                        );
                    }
                    driver = GraphDatabase.driver(URI, AuthTokens.basic(USERNAME, password));
                }
            }
        }
        return driver;
    }

    /**
     * Creates and returns a new Session configured for the 'insurance' database.
     *
     * @return Neo4j Session for the insurance database
     */
    public static Session getSession() {
        return getDriver().session(SessionConfig.forDatabase(DATABASE));
    }

    /**
     * Closes the Neo4j Driver and releases connection pool resources.
     */
    public static synchronized void closeDriver() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                System.err.println("Error closing Neo4j driver: " + e.getMessage());
            } finally {
                driver = null;
            }
        }
    }

    /**
     * Verifies connectivity to the Neo4j instance and executes a read-only
     * test query ('RETURN 1 AS ok') against the 'insurance' database.
     *
     * @return true if the connection and verification query succeed, false otherwise
     */
    public static boolean verifyConnection() {
        try {
            Driver d = getDriver();
            d.verifyConnectivity();
            try (Session session = getSession()) {
                Result result = session.run("RETURN 1 AS ok");
                if (result.hasNext()) {
                    Record record = result.next();
                    return record.get("ok").asInt() == 1;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Neo4j connection verification failed: " + e.getMessage());
            return false;
        }
    }
}
