package service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import db.ElasticsearchConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ElasticsearchIndexSetup - Idempotent index and schema mapping provisioner.
 * Sets up explicit schemas for all insurance system entities:
 * - insurance_customers
 * - insurance_policies
 * - insurance_claims
 * - insurance_payments
 * - insurance_assets
 * - insurance_service_providers
 *
 * Safe & Idempotent: Never deletes or recreates existing indexes (preventing data loss).
 * Inspects existing mappings and reports any incompatible schema definitions.
 */
public class ElasticsearchIndexSetup {

    public static final String INDEX_CUSTOMERS = "insurance_customers";
    public static final String INDEX_POLICIES = "insurance_policies";
    public static final String INDEX_CLAIMS = "insurance_claims";
    public static final String INDEX_PAYMENTS = "insurance_payments";
    public static final String INDEX_ASSETS = "insurance_assets";
    public static final String INDEX_PROVIDERS = "insurance_service_providers";

    public static final List<String> ALL_INDEXES = List.of(
        INDEX_CUSTOMERS,
        INDEX_POLICIES,
        INDEX_CLAIMS,
        INDEX_PAYMENTS,
        INDEX_ASSETS,
        INDEX_PROVIDERS
    );

    public enum IndexStatus {
        CREATED,
        EXISTS_COMPATIBLE,
        INCOMPATIBLE,
        FAILED
    }

    public static class IndexReport {
        private final String indexName;
        private final IndexStatus status;
        private final String message;
        private final List<String> discrepancies;

        public IndexReport(String indexName, IndexStatus status, String message, List<String> discrepancies) {
            this.indexName = indexName;
            this.status = status;
            this.message = message;
            this.discrepancies = discrepancies != null ? discrepancies : Collections.emptyList();
        }

        public String getIndexName() { return indexName; }
        public IndexStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public List<String> getDiscrepancies() { return discrepancies; }
    }

    public static class SetupSummary {
        private final Map<String, IndexReport> reports = new LinkedHashMap<>();

        public void addReport(IndexReport report) {
            reports.put(report.getIndexName(), report);
        }

        public Map<String, IndexReport> getReports() {
            return reports;
        }

        public boolean isAllSuccess() {
            return reports.values().stream()
                .allMatch(r -> r.getStatus() == IndexStatus.CREATED || r.getStatus() == IndexStatus.EXISTS_COMPATIBLE);
        }
    }

    /**
     * Idempotently configures all insurance indexes and their explicit mappings.
     *
     * @param client Active ElasticsearchClient
     * @return SetupSummary containing per-index provisioning reports
     */
    public static SetupSummary setupIndexes(ElasticsearchClient client) {
        SetupSummary summary = new SetupSummary();

        summary.addReport(setupCustomerIndex(client));
        summary.addReport(setupPolicyIndex(client));
        summary.addReport(setupClaimIndex(client));
        summary.addReport(setupPaymentIndex(client));
        summary.addReport(setupAssetIndex(client));
        summary.addReport(setupProviderIndex(client));

        return summary;
    }

    // ----------------------------------------------------------------------------------
    // 1. Customer Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupCustomerIndex(ElasticsearchClient client) {
        String indexName = INDEX_CUSTOMERS;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("personId", "keyword");
        expectedFields.put("firstName", "text");
        expectedFields.put("lastName", "text");
        expectedFields.put("dob", "date");
        expectedFields.put("email", "keyword");
        expectedFields.put("aadhar", "keyword");
        expectedFields.put("address", "text");
        expectedFields.put("policyIds", "keyword");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("personId", p -> p.keyword(k -> k))
                    .properties("firstName", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("lastName", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("dob", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("email", p -> p.keyword(k -> k))
                    .properties("aadhar", p -> p.keyword(k -> k))
                    .properties("address", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("policyIds", p -> p.keyword(k -> k))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // 2. Policy Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupPolicyIndex(ElasticsearchClient client) {
        String indexName = INDEX_POLICIES;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("policyNo", "keyword");
        expectedFields.put("startDate", "date");
        expectedFields.put("expiryDate", "date");
        expectedFields.put("auditId", "keyword");
        expectedFields.put("premiumAmount", "double");
        expectedFields.put("customerIds", "keyword");
        expectedFields.put("assetIds", "keyword");
        expectedFields.put("claimIds", "keyword");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("policyNo", p -> p.keyword(k -> k))
                    .properties("startDate", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("expiryDate", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("auditId", p -> p.keyword(k -> k))
                    .properties("premiumAmount", p -> p.double_(d -> d))
                    .properties("customerIds", p -> p.keyword(k -> k))
                    .properties("assetIds", p -> p.keyword(k -> k))
                    .properties("claimIds", p -> p.keyword(k -> k))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // 3. Claim Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupClaimIndex(ElasticsearchClient client) {
        String indexName = INDEX_CLAIMS;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("claimId", "keyword");
        expectedFields.put("policyNo", "keyword");
        expectedFields.put("incidentDate", "date");
        expectedFields.put("description", "text");
        expectedFields.put("claimedAmount", "double");
        expectedFields.put("status", "keyword");
        expectedFields.put("paymentIds", "keyword");
        expectedFields.put("providerIds", "keyword");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("claimId", p -> p.keyword(k -> k))
                    .properties("policyNo", p -> p.keyword(k -> k))
                    .properties("incidentDate", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("description", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("claimedAmount", p -> p.double_(d -> d))
                    .properties("status", p -> p.keyword(k -> k))
                    .properties("paymentIds", p -> p.keyword(k -> k))
                    .properties("providerIds", p -> p.keyword(k -> k))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // 4. Payment Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupPaymentIndex(ElasticsearchClient client) {
        String indexName = INDEX_PAYMENTS;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("claimId", "keyword");
        expectedFields.put("paymentId", "keyword");
        expectedFields.put("paymentDate", "date");
        expectedFields.put("paymentMethod", "keyword");
        expectedFields.put("amountPaid", "double");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("claimId", p -> p.keyword(k -> k))
                    .properties("paymentId", p -> p.keyword(k -> k))
                    .properties("paymentDate", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("paymentMethod", p -> p.keyword(k -> k))
                    .properties("amountPaid", p -> p.double_(d -> d))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // 5. Asset Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupAssetIndex(ElasticsearchClient client) {
        String indexName = INDEX_ASSETS;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("assetId", "keyword");
        expectedFields.put("description", "text");
        expectedFields.put("purchaseDate", "date");
        expectedFields.put("estimatedValue", "double");
        expectedFields.put("policyIds", "keyword");
        expectedFields.put("providerIds", "keyword");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("assetId", p -> p.keyword(k -> k))
                    .properties("description", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("purchaseDate", p -> p.date(d -> d.format("yyyy-MM-dd||strict_date_optional_time||epoch_millis")))
                    .properties("estimatedValue", p -> p.double_(d -> d))
                    .properties("policyIds", p -> p.keyword(k -> k))
                    .properties("providerIds", p -> p.keyword(k -> k))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // 6. Service Provider Index
    // ----------------------------------------------------------------------------------
    public static IndexReport setupProviderIndex(ElasticsearchClient client) {
        String indexName = INDEX_PROVIDERS;
        Map<String, String> expectedFields = new LinkedHashMap<>();
        expectedFields.put("providerId", "keyword");
        expectedFields.put("name", "text");
        expectedFields.put("bankAccount", "keyword");
        expectedFields.put("phones", "keyword");
        expectedFields.put("claimIds", "keyword");
        expectedFields.put("assetIds", "keyword");

        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return inspectExistingMapping(client, indexName, expectedFields);
            }

            CreateIndexResponse res = client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("providerId", p -> p.keyword(k -> k))
                    .properties("name", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("bankAccount", p -> p.keyword(k -> k))
                    .properties("phones", p -> p.keyword(k -> k))
                    .properties("claimIds", p -> p.keyword(k -> k))
                    .properties("assetIds", p -> p.keyword(k -> k))
                )
            );

            return new IndexReport(indexName, IndexStatus.CREATED, "Index created successfully (acknowledged: " + res.acknowledged() + ")", null);
        } catch (Exception e) {
            return new IndexReport(indexName, IndexStatus.FAILED, "Index setup failed: " + ElasticsearchConnection.sanitize(e.getMessage()), null);
        }
    }

    // ----------------------------------------------------------------------------------
    // Existing Mapping Inspection & Compatibility Verification
    // ----------------------------------------------------------------------------------
    private static IndexReport inspectExistingMapping(
            ElasticsearchClient client,
            String indexName,
            Map<String, String> expectedFields) throws IOException {

        GetMappingResponse mappingResponse = client.indices().getMapping(m -> m.index(indexName));
        IndexMappingRecord record = mappingResponse.get(indexName);

        if (record == null || record.mappings() == null || record.mappings().properties() == null) {
            return new IndexReport(
                indexName,
                IndexStatus.EXISTS_COMPATIBLE,
                "Index exists with uninitialized/empty mappings.",
                null
            );
        }

        Map<String, Property> existingProps = record.mappings().properties();
        List<String> discrepancies = new ArrayList<>();

        for (Map.Entry<String, String> expected : expectedFields.entrySet()) {
            String fieldName = expected.getKey();
            String expectedType = expected.getValue();

            Property prop = existingProps.get(fieldName);
            if (prop == null) {
                discrepancies.add("Missing expected field '" + fieldName + "' (expected " + expectedType + ")");
            } else {
                String actualType = prop._kind() != null ? prop._kind().jsonValue() : "unknown";
                if (!typesCompatible(expectedType, actualType)) {
                    discrepancies.add("Field '" + fieldName + "' type mismatch: expected " + expectedType + " but found " + actualType);
                }
            }
        }

        if (discrepancies.isEmpty()) {
            return new IndexReport(
                indexName,
                IndexStatus.EXISTS_COMPATIBLE,
                "Index already exists and has compatible schema mapping.",
                null
            );
        } else {
            return new IndexReport(
                indexName,
                IndexStatus.INCOMPATIBLE,
                "Index exists but contains mapping discrepancies (preserved without alteration to prevent data loss).",
                discrepancies
            );
        }
    }

    private static boolean typesCompatible(String expected, String actual) {
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        // Allow text with keyword subfield or text/keyword interchangeable for string fields
        if ("text".equalsIgnoreCase(expected) && ("keyword".equalsIgnoreCase(actual) || "text".equalsIgnoreCase(actual))) {
            return true;
        }
        if ("keyword".equalsIgnoreCase(expected) && ("keyword".equalsIgnoreCase(actual) || "text".equalsIgnoreCase(actual))) {
            return true;
        }
        // Number conversions
        if ("double".equalsIgnoreCase(expected) && ("float".equalsIgnoreCase(actual) || "long".equalsIgnoreCase(actual) || "double".equalsIgnoreCase(actual))) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   ELASTICSEARCH IDEMPOTENT INDEX PROVISIONER - INSURANCE SYSTEM DA3");
        System.out.println("================================================================================\n");

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            System.out.println("Connected to Elasticsearch. Provisioning indices...\n");

            SetupSummary summary = setupIndexes(client);

            for (IndexReport report : summary.getReports().values()) {
                System.out.printf("[%s] %s%n", report.getStatus(), report.getIndexName());
                System.out.println("  " + report.getMessage());
                if (!report.getDiscrepancies().isEmpty()) {
                    System.out.println("  Discrepancies found:");
                    for (String d : report.getDiscrepancies()) {
                        System.out.println("    - " + d);
                    }
                }
                System.out.println();
            }

            System.out.println("================================================================================");
            if (summary.isAllSuccess()) {
                System.out.println("ALL INDEXES READY FOR DATA MIGRATION.");
            } else {
                System.out.println("SOME INDEXES ENCOUNTERED WARNINGS OR INCOMPATIBILITIES (REVIEW ABOVE).");
            }
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to run index setup: " + ElasticsearchConnection.sanitize(e.getMessage()));
        } finally {
            ElasticsearchConnection.closeClient();
        }
    }
}
