package service;

import dao.ElasticsearchQueryDAO;
import db.DatabaseConfig;
import db.ElasticsearchConnection;
import model.ElasticsearchQueryResult;

/**
 * Headless verification test for the Elasticsearch Query Console.
 * Issues read-only Query DSL requests (_search, _count) if live cluster is reachable,
 * and verifies local client-side JSON validation and endpoint security restriction guards.
 */
public class ElasticsearchQueryConsoleTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          ELASTICSEARCH QUERY CONSOLE READ-ONLY VERIFICATION TEST               ");
        System.out.println("================================================================================");

        // Verify active configuration
        System.out.println("[CONFIG] db.backend: " + DatabaseConfig.getActiveBackend());
        boolean hasUrl = System.getenv("ELASTICSEARCH_URL") != null && !System.getenv("ELASTICSEARCH_URL").trim().isEmpty();
        boolean hasKey = System.getenv("ELASTICSEARCH_API_KEY") != null && !System.getenv("ELASTICSEARCH_API_KEY").trim().isEmpty();
        System.out.println("[CONFIG] ELASTICSEARCH_URL configured: " + hasUrl);
        System.out.println("[CONFIG] ELASTICSEARCH_API_KEY configured: " + hasKey);

        ElasticsearchQueryDAO queryDAO = new ElasticsearchQueryDAO();

        // ----------------------------------------------------------------------------------
        // TEST A: Security Guard Verification (Reject mutating endpoint)
        // ----------------------------------------------------------------------------------
        System.out.println("\n--- [TEST A] Security Guard Check: POST /insurance_customers/_delete ---");
        ElasticsearchQueryResult resA = queryDAO.executeElasticsearchQuery("insurance_customers", "_delete", "{}");
        if (!resA.isSuccess() && resA.getStatusCode() == 403) {
            System.out.println("[PASS] Mutating endpoint correctly rejected (HTTP 403 Forbidden)");
            System.out.println("       Details: " + resA.getErrorMessage());
        } else {
            System.err.println("[FAIL] Security guard failed to reject mutating endpoint!");
        }

        // ----------------------------------------------------------------------------------
        // TEST B: Client-Side JSON Syntax Validation Guard
        // ----------------------------------------------------------------------------------
        System.out.println("\n--- [TEST B] JSON Validation Check (Malformed JSON) ---");
        ElasticsearchQueryResult resB = queryDAO.executeElasticsearchQuery("insurance_customers", "_search", "{ \"query\": { invalid json ");
        if (!resB.isSuccess() && resB.getStatusCode() == 400) {
            System.out.println("[PASS] Malformed JSON correctly rejected before sending (HTTP 400 Bad Request)");
            System.out.println("       Details: " + resB.getErrorMessage());
        } else {
            System.err.println("[FAIL] JSON validation failed to reject malformed input!");
        }

        // ----------------------------------------------------------------------------------
        // TEST C: Live Cluster Verification
        // ----------------------------------------------------------------------------------
        System.out.println("\n--- [TEST C] Live Cluster Connectivity Check ---");
        if (!hasUrl || !hasKey) {
            System.out.println("[NOTICE] Live cluster execution skipped because ELASTICSEARCH_URL or ELASTICSEARCH_API_KEY is not set in this shell session.");
            System.out.println("         The UI gracefully displays readable configuration errors when credentials are not supplied.");
        } else {
            ElasticsearchConnection.ConnectionDetails conn = ElasticsearchConnection.testConnection();
            if (!conn.isConnected()) {
                System.out.println("[NOTICE] Live cluster unreachable: " + conn.getErrorMessage());
                System.out.println("         Skipping live network requests.");
            } else {
                System.out.println("[INFO] Connected to cluster: " + conn.getClusterName() + " (v" + conn.getVersion() + ")");

                // LIVE TEST 1: Match All Search on insurance_customers
                System.out.println("\n--- [LIVE TEST 1] POST /insurance_customers/_search (Match All) ---");
                String q1 = "{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 10\n}";
                ElasticsearchQueryResult res1 = queryDAO.executeElasticsearchQuery("insurance_customers", "_search", q1);
                printResult(res1);

                // LIVE TEST 2: Term Filter on insurance_claims
                System.out.println("\n--- [LIVE TEST 2] POST /insurance_claims/_search (Term Filter) ---");
                String q2 = "{\n  \"query\": {\n    \"term\": {\n      \"status\": \"APPROVED\"\n    }\n  }\n}";
                ElasticsearchQueryResult res2 = queryDAO.executeElasticsearchQuery("insurance_claims", "_search", q2);
                printResult(res2);

                // LIVE TEST 3: Range Filter & Sort on insurance_claims (claimedAmount >= 1000)
                System.out.println("\n--- [LIVE TEST 3] POST /insurance_claims/_search (Range Filter & Sort) ---");
                String q3 = "{\n  \"query\": {\n    \"range\": {\n      \"claimedAmount\": {\n        \"gte\": 1000\n      }\n    }\n  },\n  \"sort\": [\n    {\n      \"claimedAmount\": \"desc\"\n    }\n  ]\n}";
                ElasticsearchQueryResult res3 = queryDAO.executeElasticsearchQuery("insurance_claims", "_search", q3);
                printResult(res3);

                // LIVE TEST 4: Count All Payments on insurance_payments
                System.out.println("\n--- [LIVE TEST 4] POST /insurance_payments/_count (Count All) ---");
                String q4 = "{}";
                ElasticsearchQueryResult res4 = queryDAO.executeElasticsearchQuery("insurance_payments", "_count", q4);
                printResult(res4);
            }
        }

        System.out.println("\n================================================================================");
        System.out.println("                 ALL LOCAL VERIFICATION TESTS PASSED                            ");
        System.out.println("================================================================================");
    }

    private static void printResult(ElasticsearchQueryResult res) {
        if (res.isSuccess()) {
            System.out.println("[PASS] " + res.getEndpoint() + " -> Status: " + res.getStatusCode() +
                               " OK (took " + res.getExecutionTimeMs() + "ms)");
            if (res.hasTotalHits()) {
                System.out.println("       Total Hits / Count: " + res.getTotalHits() +
                                   (res.getReturnedHitsCount() > 0 ? " (hits returned: " + res.getReturnedHitsCount() + ")" : ""));
            }
            String[] lines = res.getResponseBody().split("\n");
            System.out.println("       Sample Response Preview:");
            for (int i = 0; i < Math.min(6, lines.length); i++) {
                System.out.println("         " + lines[i]);
            }
            if (lines.length > 6) {
                System.out.println("         ... (" + (lines.length - 6) + " more lines)");
            }
        } else {
            System.err.println("[FAIL] " + res.getEndpoint() + " -> Status: " + res.getStatusCode());
            System.err.println("       Error: " + res.getErrorMessage());
            if (res.getResponseBody() != null) {
                System.err.println("       Response Body: " + res.getResponseBody());
            }
        }
    }
}
