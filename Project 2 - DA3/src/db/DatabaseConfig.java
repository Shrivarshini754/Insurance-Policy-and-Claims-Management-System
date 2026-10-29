package db;

/**
 * Database configuration manager for the Insurance Policy Claims System.
 * Supports runtime backend selection between Neo4j and Elasticsearch
 * via system property 'db.backend' or environment variable 'DB_BACKEND'.
 * Defaults to Neo4j if neither is explicitly set.
 */
public class DatabaseConfig {

    public static final String BACKEND_SYS_PROP = "db.backend";
    public static final String BACKEND_ENV_VAR = "DB_BACKEND";

    public static final String BACKEND_NEO4J = "neo4j";
    public static final String BACKEND_ELASTICSEARCH = "elasticsearch";

    private DatabaseConfig() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Determines the active backend name.
     * Checks system property 'db.backend', then environment variable 'DB_BACKEND'.
     * Defaults to 'neo4j' if neither is configured.
     *
     * @return active backend name ("neo4j" or "elasticsearch")
     */
    public static String getActiveBackend() {
        String prop = System.getProperty(BACKEND_SYS_PROP);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim().toLowerCase();
        }
        String env = System.getenv(BACKEND_ENV_VAR);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim().toLowerCase();
        }
        return BACKEND_NEO4J;
    }

    /**
     * Checks if the active backend is configured to Elasticsearch.
     *
     * @return true if 'elasticsearch' is explicitly selected, false otherwise
     */
    public static boolean isElasticsearch() {
        return BACKEND_ELASTICSEARCH.equalsIgnoreCase(getActiveBackend());
    }

    /**
     * Checks if the active backend is Neo4j (the default).
     *
     * @return true if active backend is 'neo4j', false otherwise
     */
    public static boolean isNeo4j() {
        return !isElasticsearch();
    }
}
