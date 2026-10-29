package db;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.net.URI;

/**
 * Elasticsearch connection and client manager for Insurance Management System.
 * Manages the lifecycle of ElasticsearchClient and RestClientTransport.
 * Configured securely via HTTPS using API Key authentication read from environment variables.
 */
public class ElasticsearchConnection {

    public static final String URL_ENV_VAR = "ELASTICSEARCH_URL";
    public static final String API_KEY_ENV_VAR = "ELASTICSEARCH_API_KEY";

    private static volatile ElasticsearchClient client;
    private static volatile ElasticsearchTransport transport;
    private static volatile RestClient restClient;

    private ElasticsearchConnection() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Data carrier for connection verification results.
     */
    public static class ConnectionDetails {
        private final boolean connected;
        private final String clusterName;
        private final String version;
        private final String clusterUuid;
        private final String errorMessage;

        public ConnectionDetails(boolean connected, String clusterName, String version, String clusterUuid, String errorMessage) {
            this.connected = connected;
            this.clusterName = clusterName;
            this.version = version;
            this.clusterUuid = clusterUuid;
            this.errorMessage = errorMessage;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getClusterName() {
            return clusterName;
        }

        public String getVersion() {
            return version;
        }

        public String getClusterUuid() {
            return clusterUuid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Sanitizes messages to prevent credentials or sensitive API key tokens from leaking into logs.
     *
     * @param input Raw message or exception string
     * @return Sanitized string with sensitive tokens redacted
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String sanitized = input;
        String apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            sanitized = sanitized.replace(apiKey.trim(), "[REDACTED_API_KEY]");
        }
        // Redact Authorization headers or raw ApiKey tokens that might appear in logs or HTTP exceptions
        sanitized = sanitized.replaceAll("(?i)(Authorization:\\s*ApiKey\\s+)[A-Za-z0-9+/=_-]+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(ApiKey\\s+)[A-Za-z0-9+/=_-]{12,}", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key[\"':\\s=]+)[A-Za-z0-9+/=_-]{12,}", "$1[REDACTED]");
        return sanitized;
    }

    /**
     * Obtains the singleton ElasticsearchClient instance, initializing it lazily if needed.
     * Reads ELASTICSEARCH_URL and ELASTICSEARCH_API_KEY from environment variables.
     * Enforces HTTPS protocol for secure communication.
     *
     * @return active ElasticsearchClient instance
     * @throws IllegalStateException if required environment variables are missing
     * @throws IllegalArgumentException if the URL does not use HTTPS
     */
    public static ElasticsearchClient getClient() {
        if (client == null) {
            synchronized (ElasticsearchConnection.class) {
                if (client == null) {
                    String url = System.getenv(URL_ENV_VAR);
                    if (url == null || url.trim().isEmpty()) {
                        throw new IllegalStateException(
                            "Environment variable '" + URL_ENV_VAR + "' is not set. " +
                            "Please configure '" + URL_ENV_VAR + "' with your Elasticsearch HTTPS endpoint."
                        );
                    }
                    url = url.trim().replaceAll("/+$", "");

                    try {
                        URI uri = URI.create(url);
                        if (!"https".equalsIgnoreCase(uri.getScheme())) {
                            throw new IllegalArgumentException(
                                "Insecure connection rejected: '" + URL_ENV_VAR +
                                "' must use HTTPS protocol scheme (e.g., https://your-deployment.es.io:443)."
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid URI format for '" + URL_ENV_VAR + "': " + sanitize(e.getMessage()));
                    }

                    String apiKey = System.getenv(API_KEY_ENV_VAR);
                    if (apiKey == null || apiKey.trim().isEmpty()) {
                        throw new IllegalStateException(
                            "Environment variable '" + API_KEY_ENV_VAR + "' is not set. " +
                            "Please configure '" + API_KEY_ENV_VAR + "' with your Elasticsearch API key."
                        );
                    }
                    apiKey = apiKey.trim();

                    Header[] defaultHeaders = new Header[] {
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                    };

                    restClient = RestClient.builder(HttpHost.create(url))
                        .setDefaultHeaders(defaultHeaders)
                        .setRequestConfigCallback(requestConfigBuilder ->
                            requestConfigBuilder
                                .setConnectTimeout(10000)
                                .setSocketTimeout(30000)
                        )
                        .build();

                    transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
                    client = new ElasticsearchClient(transport);
                }
            }
        }
        return client;
    }

    /**
     * Obtains the underlying low-level RestClient instance, initializing it lazily if needed.
     * Useful for performing direct read-only Query DSL requests (_search, _count).
     *
     * @return active RestClient instance
     */
    public static RestClient getRestClient() {
        if (restClient == null) {
            getClient();
        }
        return restClient;
    }

    /**
     * Closes the active Elasticsearch client and releases underlying network and transport resources.
     */
    public static synchronized void closeClient() {
        if (transport != null) {
            try {
                transport.close();
            } catch (IOException e) {
                System.err.println("Error closing Elasticsearch transport: " + sanitize(e.getMessage()));
            } finally {
                transport = null;
            }
        }
        if (restClient != null) {
            try {
                restClient.close();
            } catch (IOException e) {
                System.err.println("Error closing Elasticsearch RestClient: " + sanitize(e.getMessage()));
            } finally {
                restClient = null;
            }
        }
        client = null;
    }

    /**
     * Performs a non-destructive cluster health and connectivity check.
     * Calls ping() and info() to verify authentication and reachability.
     *
     * @return ConnectionDetails containing cluster name, version, and status or sanitized error
     */
    public static ConnectionDetails testConnection() {
        try {
            ElasticsearchClient esClient = getClient();
            BooleanResponse ping = esClient.ping();
            if (ping == null || !ping.value()) {
                return new ConnectionDetails(false, null, null, null, "Cluster ping responded with false.");
            }
            InfoResponse info = esClient.info();
            String clusterName = info.clusterName();
            String version = (info.version() != null) ? info.version().number() : "unknown";
            String uuid = info.clusterUuid();
            return new ConnectionDetails(true, clusterName, version, uuid, null);
        } catch (Exception e) {
            String sanitizedError = sanitize(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return new ConnectionDetails(false, null, null, null, sanitizedError);
        }
    }

    /**
     * Verifies connectivity to the Elasticsearch instance.
     *
     * @return true if ping and info calls succeed, false otherwise
     */
    public static boolean verifyConnection() {
        ConnectionDetails details = testConnection();
        if (!details.isConnected()) {
            System.err.println("Elasticsearch connection verification failed: " + details.getErrorMessage());
        }
        return details.isConnected();
    }
}
