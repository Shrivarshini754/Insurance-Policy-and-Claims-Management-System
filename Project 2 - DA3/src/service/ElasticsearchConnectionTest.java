package service;

import db.ElasticsearchConnection;
import db.ElasticsearchConnection.ConnectionDetails;

/**
 * ElasticsearchConnectionTest - Standalone connection test runner.
 * Verifies environment variable configuration, HTTPS enforcement, and cluster reachability.
 * Reports cluster details upon success or a sanitized error message upon failure.
 */
public class ElasticsearchConnectionTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("       ELASTICSEARCH CLOUD CONNECTION TEST - INSURANCE SYSTEM DA3");
        System.out.println("================================================================================\n");

        String url = System.getenv(ElasticsearchConnection.URL_ENV_VAR);
        String apiKey = System.getenv(ElasticsearchConnection.API_KEY_ENV_VAR);

        boolean envMissing = false;

        if (url == null || url.trim().isEmpty()) {
            System.out.println("[MISSING] Environment variable '" + ElasticsearchConnection.URL_ENV_VAR + "' is not set.");
            envMissing = true;
        } else {
            System.out.println("[CONFIG] " + ElasticsearchConnection.URL_ENV_VAR + " = " + ElasticsearchConnection.sanitize(url.trim()));
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[MISSING] Environment variable '" + ElasticsearchConnection.API_KEY_ENV_VAR + "' is not set.");
            envMissing = true;
        } else {
            System.out.println("[CONFIG] " + ElasticsearchConnection.API_KEY_ENV_VAR + " = [CONFIGURED - " + apiKey.trim().length() + " chars, REDACTED]");
        }

        if (envMissing) {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("HOW TO CONFIGURE ENVIRONMENT VARIABLES LOCALLY:");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("PowerShell (Current Session):");
            System.out.println("  $env:ELASTICSEARCH_URL=\"https://your-deployment-endpoint.es.region.cloud.es.io:443\"");
            System.out.println("  $env:ELASTICSEARCH_API_KEY=\"your_base64_encoded_api_key\"\n");
            System.out.println("Windows CMD (Current Session):");
            System.out.println("  set ELASTICSEARCH_URL=https://your-deployment-endpoint.es.region.cloud.es.io:443");
            System.out.println("  set ELASTICSEARCH_API_KEY=your_base64_encoded_api_key\n");
            System.out.println("Windows Persistent (User Profile):");
            System.out.println("  setx ELASTICSEARCH_URL \"https://your-deployment-endpoint.es.region.cloud.es.io:443\"");
            System.out.println("  setx ELASTICSEARCH_API_KEY \"your_base64_encoded_api_key\"\n");
            System.out.println("Linux / macOS (Bash / Zsh):");
            System.out.println("  export ELASTICSEARCH_URL=\"https://your-deployment-endpoint.es.region.cloud.es.io:443\"");
            System.out.println("  export ELASTICSEARCH_API_KEY=\"your_base64_encoded_api_key\"");
            System.out.println("================================================================================\n");
            return;
        }

        System.out.println("\n>>> Testing connection to Elasticsearch cluster...");

        try {
            ConnectionDetails details = ElasticsearchConnection.testConnection();

            if (details.isConnected()) {
                System.out.println("\n[SUCCESS] Successfully connected to Elasticsearch cluster!");
                System.out.println("  Cluster Name : " + details.getClusterName());
                System.out.println("  ES Version   : " + details.getVersion());
                System.out.println("  Cluster UUID : " + details.getClusterUuid());
                System.out.println("================================================================================");
            } else {
                System.out.println("\n[ERROR] Connection test failed.");
                System.out.println("  Reason: " + details.getErrorMessage());
                System.out.println("================================================================================");
            }
        } finally {
            ElasticsearchConnection.closeClient();
        }
    }
}
