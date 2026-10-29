package service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import db.ElasticsearchConnection;
import db.Neo4jConnection;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ElasticsearchMigrationRunner - Robust, non-destructive migration engine.
 * Reads source data from Neo4j in read-only mode, extracts graph edges and foreign-key properties,
 * synthesizes bidirectional relationships, and prepares/writes documents into Elasticsearch.
 *
 * Safety & Quality Guarantees:
 * 1. STRICTLY READ-ONLY against Neo4j: Uses only read-only Cypher queries. Never alters the graph.
 * 2. Independent Pattern Comprehensions: Completely avoids chained OPTIONAL MATCH Cartesian explosion.
 * 3. Hybrid Relationship Synthesis: Unifies graph edges AND verified foreign-key properties.
 * 4. Deterministic Document IDs: Composite keys for payments (claimId_paymentId) prevent collisions.
 * 5. Dynamic Schema & Provider Discovery: Discovers labels/types dynamically without inventing records.
 * 6. Extended Referential Validation: Detects both broken references AND missing expected relationships.
 * 7. Dry-Run Default: Zero writes to Elasticsearch unless explicitly passed --execute.
 * 8. Clean Resource Teardown: Properly releases Driver and Client without disruptive System.exit().
 */
public class ElasticsearchMigrationRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static class EntityMigrationStats {
        private final String entityName;
        private final String indexName;
        private int readCount = 0;
        private int validCount = 0;
        private int duplicateCount = 0;
        private int missingRefCount = 0;
        private int emptyRelationshipCount = 0;
        private int indexedCount = 0;
        private int skippedCount = 0;
        private int failedCount = 0;
        private final List<String> issues = new ArrayList<>();
        private final List<String> notices = new ArrayList<>();

        public EntityMigrationStats(String entityName, String indexName) {
            this.entityName = entityName;
            this.indexName = indexName;
        }

        public String getEntityName() { return entityName; }
        public String getIndexName() { return indexName; }
        public int getReadCount() { return readCount; }
        public int getValidCount() { return validCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public int getMissingRefCount() { return missingRefCount; }
        public int getEmptyRelationshipCount() { return emptyRelationshipCount; }
        public int getIndexedCount() { return indexedCount; }
        public int getSkippedCount() { return skippedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getIssues() { return issues; }
        public List<String> getNotices() { return notices; }

        public void addIssue(String issue) { issues.add(issue); }
        public void addNotice(String notice) { notices.add(notice); }
    }

    public static class GraphSchemaMetadata {
        public final Set<String> labels = new LinkedHashSet<>();
        public final Set<String> relationshipTypes = new LinkedHashSet<>();
        public final Map<String, Integer> relationshipTypeCounts = new LinkedHashMap<>();
        public final Map<String, Integer> serviceProviderLabelCounts = new LinkedHashMap<>();
        public int totalNodes = 0;
        public int totalRelationships = 0;
    }

    public static class MigrationDataset {
        public final List<Map<String, Object>> customers = new ArrayList<>();
        public final List<Map<String, Object>> policies = new ArrayList<>();
        public final List<Map<String, Object>> claims = new ArrayList<>();
        public final List<Map<String, Object>> payments = new ArrayList<>();
        public final List<Map<String, Object>> assets = new ArrayList<>();
        public final List<Map<String, Object>> providers = new ArrayList<>();

        public final Map<String, EntityMigrationStats> statsMap = new LinkedHashMap<>();
        public final GraphSchemaMetadata schemaMeta = new GraphSchemaMetadata();

        public MigrationDataset() {
            statsMap.put("Customer", new EntityMigrationStats("Customer", ElasticsearchIndexSetup.INDEX_CUSTOMERS));
            statsMap.put("Policy", new EntityMigrationStats("Policy", ElasticsearchIndexSetup.INDEX_POLICIES));
            statsMap.put("Claim", new EntityMigrationStats("Claim", ElasticsearchIndexSetup.INDEX_CLAIMS));
            statsMap.put("Payment", new EntityMigrationStats("Payment", ElasticsearchIndexSetup.INDEX_PAYMENTS));
            statsMap.put("Asset", new EntityMigrationStats("Asset", ElasticsearchIndexSetup.INDEX_ASSETS));
            statsMap.put("ServiceProvider", new EntityMigrationStats("ServiceProvider", ElasticsearchIndexSetup.INDEX_PROVIDERS));
        }
    }

    // ==================================================================================
    // SOURCE EXTRACTOR (Strictly Read-Only Queries against Neo4j)
    // ==================================================================================

    public static MigrationDataset extractFromNeo4j() {
        MigrationDataset dataset = new MigrationDataset();

        try (Session session = Neo4jConnection.getSession()) {
            session.executeRead(tx -> {
                // Step 0: Dynamic Schema & Metadata Discovery
                discoverGraphSchema(tx, dataset.schemaMeta);

                // 1. Read Customers using safe pattern comprehension (no Cartesian product)
                String custCypher =
                    "MATCH (c:Customer) " +
                    "RETURN coalesce(c.personId, c.Person_ID, '') AS personId, " +
                    "       coalesce(c.firstName, c.F_Name, '') AS firstName, " +
                    "       coalesce(c.lastName, c.L_Name, '') AS lastName, " +
                    "       coalesce(toString(c.dob), toString(c.DOB), '') AS dob, " +
                    "       coalesce(c.email, c.Email, '') AS email, " +
                    "       coalesce(c.aadhar, c.Aadhar, '') AS aadhar, " +
                    "       coalesce(c.address, c.Address, '') AS address, " +
                    "       [(c)-[r]-(p:Policy) | coalesce(p.policyNo, p.P_No, '')] AS graphPolicyIds " +
                    "ORDER BY personId";

                Result custResult = tx.run(custCypher);
                while (custResult.hasNext()) {
                    Record r = custResult.next();
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("personId", r.get("personId", ""));
                    doc.put("firstName", r.get("firstName", ""));
                    doc.put("lastName", r.get("lastName", ""));
                    String dob = r.get("dob", "");
                    if (!dob.isEmpty()) doc.put("dob", dob);
                    doc.put("email", r.get("email", ""));
                    doc.put("aadhar", r.get("aadhar", ""));
                    doc.put("address", r.get("address", ""));
                    doc.put("policyIds", toStringList(r.get("graphPolicyIds")));
                    dataset.customers.add(doc);
                }

                // 2. Read Policies using independent pattern comprehensions
                String polCypher =
                    "MATCH (p:Policy) " +
                    "RETURN coalesce(p.policyNo, p.P_No, '') AS policyNo, " +
                    "       coalesce(toString(p.startDate), toString(p.Start_Date), '') AS startDate, " +
                    "       coalesce(toString(p.expiryDate), toString(p.Expiry_Date), '') AS expiryDate, " +
                    "       coalesce(p.auditId, p.Audit_ID, '') AS auditId, " +
                    "       coalesce(p.premiumAmount, p.Premium_Amt, 0.0) AS premiumAmount, " +
                    "       coalesce(p.personId, p.Person_ID, p.customerId, p.Customer_ID, '') AS propCustomerId, " +
                    "       coalesce(p.assetId, p.Asset_ID, '') AS propAssetId, " +
                    "       [(p)-[r]-(c:Customer) | coalesce(c.personId, c.Person_ID, '')] AS graphCustomerIds, " +
                    "       [(p)-[r]-(a:Asset) | coalesce(a.assetId, a.Asset_ID, '')] AS graphAssetIds, " +
                    "       [(p)-[r]-(cl:Claim) | coalesce(cl.claimId, cl.Claim_ID, '')] AS graphClaimIds " +
                    "ORDER BY policyNo";

                Result polResult = tx.run(polCypher);
                while (polResult.hasNext()) {
                    Record r = polResult.next();
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("policyNo", r.get("policyNo", ""));
                    String sDate = r.get("startDate", "");
                    if (!sDate.isEmpty()) doc.put("startDate", sDate);
                    String eDate = r.get("expiryDate", "");
                    if (!eDate.isEmpty()) doc.put("expiryDate", eDate);
                    doc.put("auditId", r.get("auditId", ""));
                    doc.put("premiumAmount", r.get("premiumAmount").asDouble(0.0));

                    List<String> custIds = toStringList(r.get("graphCustomerIds"));
                    String propCust = r.get("propCustomerId", "").trim();
                    if (!propCust.isEmpty() && !custIds.contains(propCust)) custIds.add(propCust);
                    doc.put("customerIds", custIds);

                    List<String> assetIds = toStringList(r.get("graphAssetIds"));
                    String propAsset = r.get("propAssetId", "").trim();
                    if (!propAsset.isEmpty() && !assetIds.contains(propAsset)) assetIds.add(propAsset);
                    doc.put("assetIds", assetIds);

                    doc.put("claimIds", toStringList(r.get("graphClaimIds")));
                    dataset.policies.add(doc);
                }

                // 3. Read Claims using independent pattern comprehensions
                String clmCypher =
                    "MATCH (c:Claim) " +
                    "RETURN coalesce(c.claimId, c.Claim_ID, '') AS claimId, " +
                    "       coalesce(c.policyNo, c.P_No, '') AS propPolicyNo, " +
                    "       coalesce(toString(c.incidentDate), toString(c.Incident_Date), '') AS incidentDate, " +
                    "       coalesce(c.description, c.Descr, '') AS description, " +
                    "       coalesce(c.claimedAmount, c.Claimed_Amt, 0.0) AS claimedAmount, " +
                    "       coalesce(c.status, c.Claim_Status, '') AS status, " +
                    "       [(c)-[r]-(p:Policy) | coalesce(p.policyNo, p.P_No, '')] AS graphPolicyIds, " +
                    "       [(c)-[r]-(pay:Payment) | coalesce(pay.paymentId, pay.Payment_ID, '')] AS graphPaymentIds, " +
                    "       [(c)-[r]-(sp) WHERE sp:ServiceProvider OR sp:ServiceProvider1 OR sp:Provider | coalesce(sp.providerId, sp.Provider_ID, '')] AS graphProviderIds " +
                    "ORDER BY claimId";

                Result clmResult = tx.run(clmCypher);
                while (clmResult.hasNext()) {
                    Record r = clmResult.next();
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("claimId", r.get("claimId", ""));

                    List<String> polIds = toStringList(r.get("graphPolicyIds"));
                    String propPol = r.get("propPolicyNo", "").trim();
                    String policyNo = !propPol.isEmpty() ? propPol : (!polIds.isEmpty() ? polIds.get(0) : "");
                    doc.put("policyNo", policyNo);

                    String incDate = r.get("incidentDate", "");
                    if (!incDate.isEmpty()) doc.put("incidentDate", incDate);
                    doc.put("description", r.get("description", ""));
                    doc.put("claimedAmount", r.get("claimedAmount").asDouble(0.0));
                    doc.put("status", r.get("status", ""));
                    doc.put("paymentIds", toStringList(r.get("graphPaymentIds")));
                    doc.put("providerIds", toStringList(r.get("graphProviderIds")));
                    dataset.claims.add(doc);
                }

                // 4. Read Payments using independent pattern comprehensions
                String payCypher =
                    "MATCH (pay:Payment) " +
                    "RETURN coalesce(pay.claimId, pay.Claim_ID, '') AS propClaimId, " +
                    "       coalesce(pay.paymentId, pay.Payment_ID, '') AS paymentId, " +
                    "       coalesce(toString(pay.paymentDate), toString(pay.P_Date), '') AS paymentDate, " +
                    "       coalesce(pay.paymentMethod, pay.P_Method, '') AS paymentMethod, " +
                    "       coalesce(pay.amountPaid, pay.Amt_Paid, 0.0) AS amountPaid, " +
                    "       [(pay)-[r]-(c:Claim) | coalesce(c.claimId, c.Claim_ID, '')] AS graphClaimIds " +
                    "ORDER BY propClaimId, paymentId";

                Result payResult = tx.run(payCypher);
                while (payResult.hasNext()) {
                    Record r = payResult.next();
                    Map<String, Object> doc = new LinkedHashMap<>();
                    List<String> clmIds = toStringList(r.get("graphClaimIds"));
                    String propClaim = r.get("propClaimId", "").trim();
                    String claimId = !propClaim.isEmpty() ? propClaim : (!clmIds.isEmpty() ? clmIds.get(0) : "");
                    String paymentId = r.get("paymentId", "");

                    doc.put("claimId", claimId);
                    doc.put("paymentId", paymentId);
                    String pDate = r.get("paymentDate", "");
                    if (!pDate.isEmpty()) doc.put("paymentDate", pDate);
                    doc.put("paymentMethod", r.get("paymentMethod", ""));
                    doc.put("amountPaid", r.get("amountPaid").asDouble(0.0));
                    dataset.payments.add(doc);
                }

                // 5. Read Assets using independent pattern comprehensions
                String astCypher =
                    "MATCH (a:Asset) " +
                    "RETURN coalesce(a.assetId, a.Asset_ID, '') AS assetId, " +
                    "       coalesce(a.description, a.Descr, '') AS description, " +
                    "       coalesce(toString(a.purchaseDate), toString(a.P_Date), '') AS purchaseDate, " +
                    "       coalesce(a.estimatedValue, a.Est_Value, 0.0) AS estimatedValue, " +
                    "       [(a)-[r]-(p:Policy) | coalesce(p.policyNo, p.P_No, '')] AS graphPolicyIds, " +
                    "       [(a)-[r]-(sp) WHERE sp:ServiceProvider OR sp:ServiceProvider1 OR sp:Provider | coalesce(sp.providerId, sp.Provider_ID, '')] AS graphProviderIds " +
                    "ORDER BY assetId";

                Result astResult = tx.run(astCypher);
                while (astResult.hasNext()) {
                    Record r = astResult.next();
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("assetId", r.get("assetId", ""));
                    doc.put("description", r.get("description", ""));
                    String pDate = r.get("purchaseDate", "");
                    if (!pDate.isEmpty()) doc.put("purchaseDate", pDate);
                    doc.put("estimatedValue", r.get("estimatedValue").asDouble(0.0));
                    doc.put("policyIds", toStringList(r.get("graphPolicyIds")));
                    doc.put("providerIds", toStringList(r.get("graphProviderIds")));
                    dataset.assets.add(doc);
                }

                // 6. Dynamic Service Provider Extraction (only if discovered in schema)
                extractDiscoveredServiceProviders(tx, dataset);

                return null;
            });
        }

        // Step 7: Synthesize & Deduplicate Bidirectional Relationships (In-Memory Hybrid Resolution)
        synthesizeAndDeduplicateRelationships(dataset);

        return dataset;
    }

    private static void discoverGraphSchema(org.neo4j.driver.TransactionContext tx, GraphSchemaMetadata meta) {
        try {
            // Discovered node labels
            Result labelsRes = tx.run("CALL db.labels() YIELD label RETURN label ORDER BY label");
            while (labelsRes.hasNext()) {
                meta.labels.add(labelsRes.next().get("label").asString());
            }

            // Discovered relationship types
            Result relRes = tx.run("CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType ORDER BY relationshipType");
            while (relRes.hasNext()) {
                meta.relationshipTypes.add(relRes.next().get("relationshipType").asString());
            }

            // Total nodes & relationships
            Result countRes = tx.run("MATCH (n) RETURN count(n) AS nodeCount");
            if (countRes.hasNext()) meta.totalNodes = countRes.next().get("nodeCount").asInt();

            Result relCountRes = tx.run("MATCH ()-[r]->() RETURN count(r) AS relCount");
            if (relCountRes.hasNext()) meta.totalRelationships = relCountRes.next().get("relCount").asInt();

            // Count per relationship type
            Result relTypeCounts = tx.run("MATCH ()-[r]->() RETURN type(r) AS relType, count(r) AS cnt ORDER BY relType");
            while (relTypeCounts.hasNext()) {
                Record r = relTypeCounts.next();
                meta.relationshipTypeCounts.put(r.get("relType").asString(), r.get("cnt").asInt());
            }

            // Check counts for candidate ServiceProvider labels
            for (String label : meta.labels) {
                if (label.matches("(?i).*(provider|service).*")) {
                    Result spCountRes = tx.run("MATCH (sp:`" + label + "`) RETURN count(sp) AS cnt");
                    if (spCountRes.hasNext()) {
                        meta.serviceProviderLabelCounts.put(label, spCountRes.next().get("cnt").asInt());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Schema discovery encountered notice: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
    }

    private static void extractDiscoveredServiceProviders(org.neo4j.driver.TransactionContext tx, MigrationDataset dataset) {
        String activeLabel = null;
        for (Map.Entry<String, Integer> entry : dataset.schemaMeta.serviceProviderLabelCounts.entrySet()) {
            if (entry.getValue() > 0) {
                activeLabel = entry.getKey();
                break;
            }
        }

        if (activeLabel == null) {
            return; // No service providers present in database; do not invent records
        }

        try {
            String cypher =
                "MATCH (sp:`" + activeLabel + "`) " +
                "RETURN coalesce(sp.providerId, sp.Provider_ID, '') AS providerId, " +
                "       coalesce(sp.name, sp.Name, '') AS name, " +
                "       coalesce(sp.bankAccount, sp.Bank_Acc, '') AS bankAccount, " +
                "       coalesce(sp.phone, sp.Phone, '') AS phone, " +
                "       [(sp)-[r]-(cl:Claim) | coalesce(cl.claimId, cl.Claim_ID, '')] AS graphClaimIds, " +
                "       [(sp)-[r]-(a:Asset) | coalesce(a.assetId, a.Asset_ID, '')] AS graphAssetIds " +
                "ORDER BY providerId";

            Result res = tx.run(cypher);
            while (res.hasNext()) {
                Record r = res.next();
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("providerId", r.get("providerId", ""));
                doc.put("name", r.get("name", ""));
                doc.put("bankAccount", r.get("bankAccount", ""));
                String phone = r.get("phone", "");
                List<String> phones = new ArrayList<>();
                if (!phone.isEmpty()) phones.add(phone);
                doc.put("phones", phones);
                doc.put("claimIds", toStringList(r.get("graphClaimIds")));
                doc.put("assetIds", toStringList(r.get("graphAssetIds")));
                dataset.providers.add(doc);
            }
        } catch (Exception e) {
            System.err.println("[WARN] ServiceProvider extraction notice: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
    }

    /**
     * Synthesizes and cross-references graph edges with verified foreign-key properties in memory.
     * Guarantees bidirectional consistency and deduplicates all relationship arrays.
     */
    @SuppressWarnings("unchecked")
    public static void synthesizeAndDeduplicateRelationships(MigrationDataset dataset) {
        Map<String, Map<String, Object>> customerMap = new LinkedHashMap<>();
        for (Map<String, Object> c : dataset.customers) customerMap.put((String) c.get("personId"), c);

        Map<String, Map<String, Object>> policyMap = new LinkedHashMap<>();
        for (Map<String, Object> p : dataset.policies) policyMap.put((String) p.get("policyNo"), p);

        Map<String, Map<String, Object>> claimMap = new LinkedHashMap<>();
        for (Map<String, Object> cl : dataset.claims) claimMap.put((String) cl.get("claimId"), cl);

        Map<String, Map<String, Object>> assetMap = new LinkedHashMap<>();
        for (Map<String, Object> a : dataset.assets) assetMap.put((String) a.get("assetId"), a);

        // 1. Claim <-> Policy Bidirectional Linkage
        for (Map<String, Object> cl : dataset.claims) {
            String claimId = (String) cl.get("claimId");
            String policyNo = (String) cl.get("policyNo");
            if (policyNo != null && !policyNo.isEmpty()) {
                Map<String, Object> pol = policyMap.get(policyNo);
                if (pol != null) {
                    List<String> claimIds = (List<String>) pol.computeIfAbsent("claimIds", k -> new ArrayList<String>());
                    if (!claimIds.contains(claimId)) claimIds.add(claimId);
                }
            }
        }
        for (Map<String, Object> pol : dataset.policies) {
            String policyNo = (String) pol.get("policyNo");
            List<String> claimIds = (List<String>) pol.get("claimIds");
            if (claimIds != null) {
                for (String cid : claimIds) {
                    Map<String, Object> cl = claimMap.get(cid);
                    if (cl != null) {
                        String existingPol = (String) cl.get("policyNo");
                        if (existingPol == null || existingPol.isEmpty()) {
                            cl.put("policyNo", policyNo);
                        }
                    }
                }
            }
        }

        // 2. Payment <-> Claim Bidirectional Linkage
        for (Map<String, Object> pay : dataset.payments) {
            String claimId = (String) pay.get("claimId");
            String paymentId = (String) pay.get("paymentId");
            if (claimId != null && !claimId.isEmpty()) {
                Map<String, Object> cl = claimMap.get(claimId);
                if (cl != null) {
                    List<String> payIds = (List<String>) cl.computeIfAbsent("paymentIds", k -> new ArrayList<String>());
                    if (!payIds.contains(paymentId)) payIds.add(paymentId);
                }
            }
        }

        // 3. Customer <-> Policy Bidirectional Linkage
        for (Map<String, Object> pol : dataset.policies) {
            String policyNo = (String) pol.get("policyNo");
            List<String> custIds = (List<String>) pol.get("customerIds");
            if (custIds != null) {
                for (String cid : custIds) {
                    Map<String, Object> c = customerMap.get(cid);
                    if (c != null) {
                        List<String> polIds = (List<String>) c.computeIfAbsent("policyIds", k -> new ArrayList<String>());
                        if (!polIds.contains(policyNo)) polIds.add(policyNo);
                    }
                }
            }
        }
        for (Map<String, Object> c : dataset.customers) {
            String personId = (String) c.get("personId");
            List<String> polIds = (List<String>) c.get("policyIds");
            if (polIds != null) {
                for (String pid : polIds) {
                    Map<String, Object> pol = policyMap.get(pid);
                    if (pol != null) {
                        List<String> custIds = (List<String>) pol.computeIfAbsent("customerIds", k -> new ArrayList<String>());
                        if (!custIds.contains(personId)) custIds.add(personId);
                    }
                }
            }
        }

        // 4. Policy <-> Asset Bidirectional Linkage
        for (Map<String, Object> pol : dataset.policies) {
            String policyNo = (String) pol.get("policyNo");
            List<String> astIds = (List<String>) pol.get("assetIds");
            if (astIds != null) {
                for (String aid : astIds) {
                    Map<String, Object> a = assetMap.get(aid);
                    if (a != null) {
                        List<String> polIds = (List<String>) a.computeIfAbsent("policyIds", k -> new ArrayList<String>());
                        if (!polIds.contains(policyNo)) polIds.add(policyNo);
                    }
                }
            }
        }
        for (Map<String, Object> a : dataset.assets) {
            String assetId = (String) a.get("assetId");
            List<String> polIds = (List<String>) a.get("policyIds");
            if (polIds != null) {
                for (String pid : polIds) {
                    Map<String, Object> pol = policyMap.get(pid);
                    if (pol != null) {
                        List<String> astIds = (List<String>) pol.computeIfAbsent("assetIds", k -> new ArrayList<String>());
                        if (!astIds.contains(assetId)) astIds.add(assetId);
                    }
                }
            }
        }

        // Final Deduplication of all arrays
        deduplicateDocArrays(dataset.customers, List.of("policyIds"));
        deduplicateDocArrays(dataset.policies, List.of("customerIds", "assetIds", "claimIds"));
        deduplicateDocArrays(dataset.claims, List.of("paymentIds", "providerIds"));
        deduplicateDocArrays(dataset.assets, List.of("policyIds", "providerIds"));
        deduplicateDocArrays(dataset.providers, List.of("claimIds", "assetIds", "phones"));
    }

    @SuppressWarnings("unchecked")
    private static void deduplicateDocArrays(List<Map<String, Object>> docs, List<String> arrayKeys) {
        for (Map<String, Object> doc : docs) {
            for (String key : arrayKeys) {
                Object val = doc.get(key);
                if (val instanceof List) {
                    List<String> original = (List<String>) val;
                    Set<String> cleanSet = new LinkedHashSet<>();
                    for (String item : original) {
                        if (item != null && !item.trim().isEmpty()) {
                            cleanSet.add(item.trim());
                        }
                    }
                    doc.put(key, new ArrayList<>(cleanSet));
                }
            }
        }
    }

    private static List<String> toStringList(Value val) {
        List<String> res = new ArrayList<>();
        if (val == null || val.isNull()) return res;
        for (Object item : val.asList()) {
            if (item != null) {
                String str = item.toString().trim();
                if (!str.isEmpty() && !res.contains(str)) {
                    res.add(str);
                }
            }
        }
        return res;
    }

    // ==================================================================================
    // EXTENDED VALIDATION & REFERENTIAL INTEGRITY CHECKER
    // ==================================================================================

    @SuppressWarnings("unchecked")
    public static void validateDataset(MigrationDataset dataset) {
        Set<String> knownCustIds = new HashSet<>();
        Set<String> knownPolNos = new HashSet<>();
        Set<String> knownClaimIds = new HashSet<>();
        Set<String> knownPaymentCompositeKeys = new HashSet<>();
        Set<String> knownAssetIds = new HashSet<>();
        Set<String> knownProviderIds = new HashSet<>();

        // 1. Validate Customers
        EntityMigrationStats custStats = dataset.statsMap.get("Customer");
        custStats.readCount = dataset.customers.size();
        for (Map<String, Object> c : dataset.customers) {
            String personId = (String) c.get("personId");
            if (personId == null || personId.trim().isEmpty()) {
                custStats.addIssue("HARD ERROR: Missing/Blank personId on Customer: " + c);
                continue;
            }
            if (!knownCustIds.add(personId)) {
                custStats.duplicateCount++;
                custStats.addIssue("HARD ERROR: Duplicate Customer personId: '" + personId + "'");
            } else {
                custStats.validCount++;
            }

            List<String> pols = (List<String>) c.get("policyIds");
            if (pols == null || pols.isEmpty()) {
                custStats.emptyRelationshipCount++;
                custStats.addNotice("Customer '" + personId + "' has no linked policies (uninsured customer).");
            }
        }

        // 2. Validate Policies
        EntityMigrationStats polStats = dataset.statsMap.get("Policy");
        polStats.readCount = dataset.policies.size();
        for (Map<String, Object> p : dataset.policies) {
            String policyNo = (String) p.get("policyNo");
            if (policyNo == null || policyNo.trim().isEmpty()) {
                polStats.addIssue("HARD ERROR: Missing/Blank policyNo on Policy: " + p);
                continue;
            }
            if (!knownPolNos.add(policyNo)) {
                polStats.duplicateCount++;
                polStats.addIssue("HARD ERROR: Duplicate Policy policyNo: '" + policyNo + "'");
            } else {
                polStats.validCount++;
            }

            List<String> custIds = (List<String>) p.get("customerIds");
            if (custIds == null || custIds.isEmpty()) {
                polStats.emptyRelationshipCount++;
                polStats.addNotice("Policy '" + policyNo + "' has no owner customer assigned.");
            } else {
                for (String cid : custIds) {
                    if (!knownCustIds.contains(cid)) {
                        polStats.missingRefCount++;
                        polStats.addIssue("HARD ERROR: Policy '" + policyNo + "' references non-existent Customer '" + cid + "'");
                    }
                }
            }

            List<String> astIds = (List<String>) p.get("assetIds");
            if (astIds == null || astIds.isEmpty()) {
                polStats.addNotice("Policy '" + policyNo + "' has no covered assets.");
            }
        }

        // 3. Validate Claims
        EntityMigrationStats clmStats = dataset.statsMap.get("Claim");
        clmStats.readCount = dataset.claims.size();
        for (Map<String, Object> cl : dataset.claims) {
            String claimId = (String) cl.get("claimId");
            if (claimId == null || claimId.trim().isEmpty()) {
                clmStats.addIssue("HARD ERROR: Missing/Blank claimId on Claim: " + cl);
                continue;
            }
            if (!knownClaimIds.add(claimId)) {
                clmStats.duplicateCount++;
                clmStats.addIssue("HARD ERROR: Duplicate Claim claimId: '" + claimId + "'");
            } else {
                clmStats.validCount++;
            }

            String polNo = (String) cl.get("policyNo");
            if (polNo == null || polNo.trim().isEmpty()) {
                clmStats.missingRefCount++;
                clmStats.addIssue("HARD ERROR: Claim '" + claimId + "' is missing parent Policy reference.");
            } else if (!knownPolNos.contains(polNo)) {
                clmStats.missingRefCount++;
                clmStats.addIssue("HARD ERROR: Claim '" + claimId + "' references non-existent Policy '" + polNo + "'");
            }

            List<String> pays = (List<String>) cl.get("paymentIds");
            if (pays == null || pays.isEmpty()) {
                clmStats.emptyRelationshipCount++;
                clmStats.addNotice("Claim '" + claimId + "' has no disbursements / payments recorded.");
            }
        }

        // 4. Validate Payments
        EntityMigrationStats payStats = dataset.statsMap.get("Payment");
        payStats.readCount = dataset.payments.size();
        for (Map<String, Object> pay : dataset.payments) {
            String claimId = (String) pay.get("claimId");
            String paymentId = (String) pay.get("paymentId");
            if (paymentId == null || paymentId.trim().isEmpty()) {
                payStats.addIssue("HARD ERROR: Missing/Blank paymentId on Payment: " + pay);
                continue;
            }

            String compositeKey = (claimId != null && !claimId.isEmpty()) ? (claimId + "_" + paymentId) : ("NO_CLAIM_" + paymentId);
            pay.put("_id", compositeKey); // Deterministic document ID

            if (!knownPaymentCompositeKeys.add(compositeKey)) {
                payStats.duplicateCount++;
                payStats.addIssue("HARD ERROR: Duplicate Payment composite key: '" + compositeKey + "'");
            } else {
                payStats.validCount++;
            }

            if (claimId == null || claimId.trim().isEmpty()) {
                payStats.missingRefCount++;
                payStats.addIssue("HARD ERROR: Payment '" + paymentId + "' is missing parent Claim reference.");
            } else if (!knownClaimIds.contains(claimId)) {
                payStats.missingRefCount++;
                payStats.addIssue("HARD ERROR: Payment '" + paymentId + "' references non-existent Claim '" + claimId + "'");
            }
        }

        // 5. Validate Assets
        EntityMigrationStats astStats = dataset.statsMap.get("Asset");
        astStats.readCount = dataset.assets.size();
        for (Map<String, Object> a : dataset.assets) {
            String assetId = (String) a.get("assetId");
            if (assetId == null || assetId.trim().isEmpty()) {
                astStats.addIssue("HARD ERROR: Missing/Blank assetId on Asset: " + a);
                continue;
            }
            if (!knownAssetIds.add(assetId)) {
                astStats.duplicateCount++;
                astStats.addIssue("HARD ERROR: Duplicate Asset assetId: '" + assetId + "'");
            } else {
                astStats.validCount++;
            }

            List<String> pols = (List<String>) a.get("policyIds");
            if (pols == null || pols.isEmpty()) {
                astStats.emptyRelationshipCount++;
                astStats.addNotice("Asset '" + assetId + "' has no covering policies.");
            }
        }

        // 6. Validate Service Providers
        EntityMigrationStats spStats = dataset.statsMap.get("ServiceProvider");
        spStats.readCount = dataset.providers.size();
        for (Map<String, Object> sp : dataset.providers) {
            String providerId = (String) sp.get("providerId");
            if (providerId == null || providerId.trim().isEmpty()) {
                spStats.addIssue("HARD ERROR: Missing/Blank providerId on ServiceProvider: " + sp);
                continue;
            }
            if (!knownProviderIds.add(providerId)) {
                spStats.duplicateCount++;
                spStats.addIssue("HARD ERROR: Duplicate ServiceProvider providerId: '" + providerId + "'");
            } else {
                spStats.validCount++;
            }
        }
    }

    // ==================================================================================
    // DRY-RUN MODE (Zero writes to Elasticsearch)
    // ==================================================================================

    public static void runDryRun() {
        System.out.println("================================================================================");
        System.out.println("   SAFE DATA MIGRATION ENGINE — DRY RUN MODE (NEO4J -> ELASTICSEARCH)");
        System.out.println("================================================================================\n");
        System.out.println("[INFO] Connecting to Neo4j in READ-ONLY mode...");

        MigrationDataset dataset = extractFromNeo4j();
        System.out.println("[INFO] Extraction complete. Validating entity relationships & referential integrity...\n");

        validateDataset(dataset);

        // Display Discovered Graph Topology
        System.out.println(">>> SOURCE GRAPH DISCOVERED TOPOLOGY:");
        System.out.println("  Total Nodes in Neo4j         : " + dataset.schemaMeta.totalNodes);
        System.out.println("  Total Relationships in Neo4j : " + dataset.schemaMeta.totalRelationships);
        System.out.println("  Discovered Node Labels       : " + dataset.schemaMeta.labels);
        System.out.println("  Discovered Relationship Types: " + dataset.schemaMeta.relationshipTypes);
        System.out.println("  Relationship Counts by Type  :");
        if (dataset.schemaMeta.relationshipTypeCounts.isEmpty()) {
            System.out.println("    (No relationships found)");
        } else {
            dataset.schemaMeta.relationshipTypeCounts.forEach((rel, cnt) ->
                System.out.println("    - [:" + rel + "] -> " + cnt + " edge(s)")
            );
        }
        System.out.println("  Service Provider Label Counts:");
        if (dataset.schemaMeta.serviceProviderLabelCounts.isEmpty()) {
            System.out.println("    (No service-provider candidate labels found in database)");
        } else {
            dataset.schemaMeta.serviceProviderLabelCounts.forEach((lbl, cnt) ->
                System.out.println("    - :" + lbl + " -> " + cnt + " node(s)")
            );
        }
        System.out.println();

        // Display Tabular Summary
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-28s | %-6s | %-6s | %-10s | %-8s%n",
            "Entity", "Target Index", "Read", "Valid", "HardErrors", "Notices");
        System.out.println("--------------------------------------------------------------------------------");

        int totalRead = 0;
        int totalValid = 0;
        int totalIssues = 0;
        int totalNotices = 0;

        for (EntityMigrationStats stats : dataset.statsMap.values()) {
            int issuesCount = stats.getIssues().size();
            int noticesCount = stats.getNotices().size();
            totalRead += stats.getReadCount();
            totalValid += stats.getValidCount();
            totalIssues += issuesCount;
            totalNotices += noticesCount;

            System.out.printf("%-18s | %-28s | %-6d | %-6d | %-10d | %-8d%n",
                stats.getEntityName(),
                stats.getIndexName(),
                stats.getReadCount(),
                stats.getValidCount(),
                issuesCount,
                noticesCount
            );
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-28s | %-6d | %-6d | %-10d | %-8d%n",
            "TOTAL", "-", totalRead, totalValid, totalIssues, totalNotices);
        System.out.println("--------------------------------------------------------------------------------\n");

        if (totalIssues > 0) {
            System.out.println(">>> HARD INTEGRITY ERRORS (Must be resolved before migration):");
            for (EntityMigrationStats stats : dataset.statsMap.values()) {
                if (!stats.getIssues().isEmpty()) {
                    System.out.println("  [" + stats.getEntityName() + "]:");
                    for (String issue : stats.getIssues()) {
                        System.out.println("    - " + issue);
                    }
                }
            }
            System.out.println();
        } else {
            System.out.println("[OK] Zero hard integrity errors or dangling references detected.\n");
        }

        if (totalNotices > 0) {
            System.out.println(">>> RELATIONAL COVERAGE NOTICES (Expected unassigned/claim-free/uninsured entities):");
            for (EntityMigrationStats stats : dataset.statsMap.values()) {
                if (!stats.getNotices().isEmpty()) {
                    System.out.println("  [" + stats.getEntityName() + "]:");
                    for (String notice : stats.getNotices()) {
                        System.out.println("    * " + notice);
                    }
                }
            }
            System.out.println();
        }

        // Document Previews (One per entity — displaying actual graph-linked records)
        System.out.println(">>> POPULATED ELASTICSEARCH DOCUMENT PREVIEWS (DETERMINISTIC ID STRATEGY):");

        // 1. Customer Preview: Prioritize customer with graph relationship (P103 purchased PO103)
        Map<String, Object> linkedCust = findFirstWithArray(dataset.customers, "policyIds");
        if (linkedCust == null && !dataset.customers.isEmpty()) linkedCust = dataset.customers.get(0);
        printPreview("Customer (Linked via PURCHASED edge)", ElasticsearchIndexSetup.INDEX_CUSTOMERS,
            linkedCust, linkedCust != null ? (String) linkedCust.get("personId") : "");

        // 2. Policy Preview: Prioritize policy with graph relationships (PO103 covers AS103, purchased by P103, has claim CL103)
        Map<String, Object> linkedPol = findFirstWithArray(dataset.policies, "customerIds", "assetIds");
        if (linkedPol == null && !dataset.policies.isEmpty()) linkedPol = dataset.policies.get(0);
        printPreview("Policy (Linked via PURCHASED, COVERS & HAS_CLAIM edges)", ElasticsearchIndexSetup.INDEX_POLICIES,
            linkedPol, linkedPol != null ? (String) linkedPol.get("policyNo") : "");

        // 3. Asset Preview: Prioritize asset with graph relationship (AS103 covered by PO103)
        Map<String, Object> linkedAst = findFirstWithArray(dataset.assets, "policyIds");
        if (linkedAst == null && !dataset.assets.isEmpty()) linkedAst = dataset.assets.get(0);
        printPreview("Asset (Linked via COVERS edge)", ElasticsearchIndexSetup.INDEX_ASSETS,
            linkedAst, linkedAst != null ? (String) linkedAst.get("assetId") : "");

        // 4. Claim Preview: (CL101 with policyNo & payments)
        printPreview("Claim (Linked via HAS_CLAIM & HAS_PAYMENT edges)", ElasticsearchIndexSetup.INDEX_CLAIMS,
            dataset.claims.isEmpty() ? null : dataset.claims.get(0),
            dataset.claims.isEmpty() ? null : (String) dataset.claims.get(0).get("claimId"));

        // 5. Payment Preview: (CL101_PAY102 with deterministic composite key)
        Map<String, Object> previewPayment = null;
        String previewPaymentId = null;
        if (!dataset.payments.isEmpty()) {
            Map<String, Object> rawPay = dataset.payments.get(0);
            previewPaymentId = (String) rawPay.getOrDefault("_id", rawPay.get("paymentId"));
            previewPayment = new LinkedHashMap<>(rawPay);
            previewPayment.remove("_id");
            previewPayment.remove("compositeId");
        }
        printPreview("Payment (Deterministic composite key)", ElasticsearchIndexSetup.INDEX_PAYMENTS,
            previewPayment,
            previewPaymentId);

        if (!dataset.providers.isEmpty()) {
            printPreview("ServiceProvider", ElasticsearchIndexSetup.INDEX_PROVIDERS,
                dataset.providers.get(0),
                (String) dataset.providers.get(0).get("providerId"));
        } else {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("Entity: ServiceProvider -> Index: " + ElasticsearchIndexSetup.INDEX_PROVIDERS);
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  (0 service provider nodes found in Neo4j source graph — skipping index preview)");
        }

        System.out.println("\n================================================================================");
        System.out.println("DRY RUN COMPLETE — ZERO WRITES PERFORMED TO ELASTICSEARCH OR NEO4J.");
        System.out.println("================================================================================");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findFirstWithArray(List<Map<String, Object>> docs, String... arrayKeys) {
        for (Map<String, Object> doc : docs) {
            for (String key : arrayKeys) {
                Object val = doc.get(key);
                if (val instanceof List && !((List<?>) val).isEmpty()) {
                    return doc;
                }
            }
        }
        return null;
    }

    private static void printPreview(String entityName, String indexName, Map<String, Object> doc, String docId) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("Entity: " + entityName + " -> Index: " + indexName + " (Deterministic _id: " + docId + ")");
        System.out.println("--------------------------------------------------------------------------------");
        if (doc == null) {
            System.out.println("  (No records found in source database)");
            return;
        }
        try {
            System.out.println(MAPPER.writeValueAsString(doc));
        } catch (Exception e) {
            System.out.println(doc);
        }
    }

    // ==================================================================================
    // LIVE MIGRATION EXECUTION (Invoked explicitly with --execute)
    // ==================================================================================

    public static void runLiveMigration() {
        System.out.println("================================================================================");
        System.out.println("   LIVE DATA MIGRATION — WRITING TO ELASTICSEARCH");
        System.out.println("================================================================================\n");

        ElasticsearchClient esClient = ElasticsearchConnection.getClient();

        System.out.println("[STEP 1/3] Ensuring Elasticsearch indexes and mappings exist...");
        ElasticsearchIndexSetup.SetupSummary setupSummary = ElasticsearchIndexSetup.setupIndexes(esClient);
        for (ElasticsearchIndexSetup.IndexReport rep : setupSummary.getReports().values()) {
            System.out.printf("  [%s] %s%n", rep.getStatus(), rep.getIndexName());
            if (rep.getStatus() == ElasticsearchIndexSetup.IndexStatus.INCOMPATIBLE) {
                System.err.println("  [ABORT] Incompatible existing mapping found on '" + rep.getIndexName() + "'. Aborting migration to protect data.");
                return;
            }
        }

        System.out.println("\n[STEP 2/3] Reading data from Neo4j (strictly read-only)...");
        MigrationDataset dataset = extractFromNeo4j();
        validateDataset(dataset);

        System.out.println("\n[STEP 3/3] Bulk indexing documents into Elasticsearch with deterministic IDs...");

        indexEntityBatch(esClient, dataset.statsMap.get("Customer"), dataset.customers, "personId");
        indexEntityBatch(esClient, dataset.statsMap.get("Policy"), dataset.policies, "policyNo");
        indexEntityBatch(esClient, dataset.statsMap.get("Claim"), dataset.claims, "claimId");
        indexEntityBatch(esClient, dataset.statsMap.get("Payment"), dataset.payments, "_id");
        indexEntityBatch(esClient, dataset.statsMap.get("Asset"), dataset.assets, "assetId");
        if (!dataset.providers.isEmpty()) {
            indexEntityBatch(esClient, dataset.statsMap.get("ServiceProvider"), dataset.providers, "providerId");
        }

        // Final Report
        System.out.println("\n================================================================================");
        System.out.println("MIGRATION EXECUTION REPORT:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-24s | %-6s | %-8s | %-8s | %-6s%n",
            "Entity", "Index", "Read", "Indexed", "Skipped", "Failed");
        System.out.println("--------------------------------------------------------------------------------");

        boolean anyFailures = false;
        for (EntityMigrationStats stats : dataset.statsMap.values()) {
            System.out.printf("%-18s | %-24s | %-6d | %-8d | %-8d | %-6d%n",
                stats.getEntityName(),
                stats.getIndexName(),
                stats.getReadCount(),
                stats.getIndexedCount(),
                stats.getSkippedCount(),
                stats.getFailedCount()
            );
            if (stats.getFailedCount() > 0) {
                anyFailures = true;
            }
        }
        System.out.println("================================================================================");

        if (anyFailures) {
            System.err.println("\n>>> DOCUMENT INDEXING FAILURES DETECTED:");
            for (EntityMigrationStats stats : dataset.statsMap.values()) {
                if (!stats.getIssues().isEmpty()) {
                    System.err.println("  [" + stats.getEntityName() + " (" + stats.getIndexName() + ")]:");
                    for (String issue : stats.getIssues()) {
                        System.err.println("    - " + issue);
                    }
                }
            }
            System.err.println();
            throw new IllegalStateException("Live migration completed with document-level failures. See details above.");
        }
    }

    // ==================================================================================
    // TARGETED RETRY: PAYMENT-ONLY MIGRATION EXECUTION
    // ==================================================================================

    public static void runPaymentOnlyMigration() {
        System.out.println("================================================================================");
        System.out.println("   TARGETED RETRY: PAYMENT-ONLY LIVE MIGRATION (NEO4J -> ELASTICSEARCH)");
        System.out.println("================================================================================\n");
        System.out.println("[TARGET] Indexing ONLY Payment records into '" + ElasticsearchIndexSetup.INDEX_PAYMENTS + "'");
        System.out.println("[SAFETY] Customers, Policies, Claims, and Assets will NOT be touched or re-indexed.\n");

        ElasticsearchClient esClient = ElasticsearchConnection.getClient();

        System.out.println("[STEP 1/3] Verifying Elasticsearch '" + ElasticsearchIndexSetup.INDEX_PAYMENTS + "' mapping...");
        ElasticsearchIndexSetup.IndexReport rep = ElasticsearchIndexSetup.setupPaymentIndex(esClient);
        System.out.printf("  [%s] %s: %s%n", rep.getStatus(), rep.getIndexName(), rep.getMessage());
        if (rep.getStatus() == ElasticsearchIndexSetup.IndexStatus.INCOMPATIBLE) {
            throw new IllegalStateException("Incompatible existing mapping found on '" + rep.getIndexName() + "'. Aborting retry.");
        }

        System.out.println("\n[STEP 2/3] Extracting and validating source data from Neo4j (strictly read-only)...");
        MigrationDataset dataset = extractFromNeo4j();
        validateDataset(dataset);

        EntityMigrationStats payStats = dataset.statsMap.get("Payment");
        System.out.println("  Read " + payStats.getReadCount() + " payment records from Neo4j source graph.");
        if (payStats.getReadCount() != 6) {
            System.err.println("  [NOTICE] Expected 6 payment records from Neo4j, found: " + payStats.getReadCount());
        }

        System.out.println("\n[STEP 3/3] Bulk indexing Payment documents with sanitized payloads & deterministic IDs...");
        indexEntityBatch(esClient, payStats, dataset.payments, "_id");

        System.out.println("\n================================================================================");
        System.out.println("PAYMENT MIGRATION EXECUTION REPORT:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-24s | %-6s | %-8s | %-8s | %-6s%n",
            "Entity", "Index", "Read", "Indexed", "Skipped", "Failed");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-18s | %-24s | %-6d | %-8d | %-8d | %-6d%n",
            payStats.getEntityName(),
            payStats.getIndexName(),
            payStats.getReadCount(),
            payStats.getIndexedCount(),
            payStats.getSkippedCount(),
            payStats.getFailedCount()
        );
        System.out.println("================================================================================");

        if (payStats.getFailedCount() > 0) {
            System.err.println("\n>>> PAYMENT INDEXING FAILURES DETECTED (" + payStats.getFailedCount() + " document(s) failed):");
            for (String issue : payStats.getIssues()) {
                System.err.println("  - " + issue);
            }
            System.err.println();
            throw new IllegalStateException("Payment migration failed: " + payStats.getFailedCount() + " out of " + payStats.getReadCount() + " payments failed to index.");
        }

        System.out.printf("%n[SUCCESS] All %d payment documents successfully indexed into '%s' with deterministic composite IDs.%n%n",
            payStats.getIndexedCount(), payStats.getIndexName());
    }

    private static void indexEntityBatch(
            ElasticsearchClient client,
            EntityMigrationStats stats,
            List<Map<String, Object>> docs,
            String idKey) {

        if (docs.isEmpty()) {
            return;
        }

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Map<String, Object> doc : docs) {
                String docId = (String) doc.get(idKey);
                if (docId == null || docId.trim().isEmpty()) {
                    stats.skippedCount++;
                    continue;
                }

                // Strip internal metadata fields from the document body so Elasticsearch does not reject reserved fields
                Map<String, Object> cleanDoc = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : doc.entrySet()) {
                    String k = entry.getKey();
                    if (!k.startsWith("_") && !k.equalsIgnoreCase("compositeId")) {
                        cleanDoc.put(k, entry.getValue());
                    }
                }

                br.operations(op -> op
                    .index(idx -> idx
                        .index(stats.getIndexName())
                        .id(docId)
                        .document(cleanDoc)
                    )
                );
            }

            BulkResponse response = client.bulk(br.build());

            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        stats.failedCount++;
                        String reason = item.error().reason();
                        stats.addIssue("Document '" + item.id() + "' failed: " + reason);
                    } else {
                        stats.indexedCount++;
                    }
                }
                System.err.printf("  [FAILED] %-16s: %d indexed, %d failed in '%s'%n",
                    stats.getEntityName(), stats.getIndexedCount(), stats.getFailedCount(), stats.getIndexName());
            } else {
                stats.indexedCount = docs.size();
                System.out.printf("  [OK] %-16s: %d indexed successfully into '%s'%n",
                    stats.getEntityName(), stats.getIndexedCount(), stats.getIndexName());
            }

        } catch (Exception e) {
            stats.failedCount += docs.size();
            stats.addIssue("Bulk index exception: " + ElasticsearchConnection.sanitize(e.getMessage()));
            System.err.println("  [ERROR] Failed to index " + stats.getEntityName() + ": " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
    }

    // ==================================================================================
    // MAIN ENTRYPOINT
    // ==================================================================================

    public static void main(String[] args) {
        boolean executeMode = false;
        boolean paymentsOnly = false;
        for (String arg : args) {
            if ("--execute".equalsIgnoreCase(arg)) {
                executeMode = true;
            } else if ("--payments-only".equalsIgnoreCase(arg) || "--entity=payment".equalsIgnoreCase(arg)) {
                paymentsOnly = true;
            } else if ("--execute-payments-only".equalsIgnoreCase(arg)) {
                executeMode = true;
                paymentsOnly = true;
            }
        }

        String neo4jPassword = System.getenv(Neo4jConnection.PASSWORD_ENV_VAR);
        String esUrl = System.getenv(ElasticsearchConnection.URL_ENV_VAR);
        String esApiKey = System.getenv(ElasticsearchConnection.API_KEY_ENV_VAR);

        boolean envMissing = false;
        if (neo4jPassword == null || neo4jPassword.trim().isEmpty()) {
            System.out.println("[MISSING] Environment variable '" + Neo4jConnection.PASSWORD_ENV_VAR + "' is not set.");
            envMissing = true;
        }
        if (executeMode) {
            if (esUrl == null || esUrl.trim().isEmpty()) {
                System.out.println("[MISSING] Environment variable '" + ElasticsearchConnection.URL_ENV_VAR + "' is not set.");
                envMissing = true;
            }
            if (esApiKey == null || esApiKey.trim().isEmpty()) {
                System.out.println("[MISSING] Environment variable '" + ElasticsearchConnection.API_KEY_ENV_VAR + "' is not set.");
                envMissing = true;
            }
        }

        if (envMissing) {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("HOW TO CONFIGURE ENVIRONMENT VARIABLES BEFORE RUNNING:");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("PowerShell (Current Session):");
            System.out.println("  $env:NEO4J_PASSWORD=\"your_neo4j_password\"");
            System.out.println("  $env:ELASTICSEARCH_URL=\"https://your-deployment.es.region.cloud.es.io:443\"");
            System.out.println("  $env:ELASTICSEARCH_API_KEY=\"your_base64_encoded_api_key\"\n");
            System.out.println("Windows CMD (Current Session):");
            System.out.println("  set NEO4J_PASSWORD=your_neo4j_password");
            System.out.println("  set ELASTICSEARCH_URL=https://your-deployment.es.region.cloud.es.io:443");
            System.out.println("  set ELASTICSEARCH_API_KEY=your_base64_encoded_api_key\n");
            System.out.println("To run DRY RUN mode (Safe, 0 writes):");
            System.out.println("  mvn exec:java \"-Dexec.mainClass=service.ElasticsearchMigrationRunner\"");
            System.out.println("To retry PAYMENTS ONLY (Live writes to insurance_payments ONLY):");
            System.out.println("  mvn exec:java \"-Dexec.mainClass=service.ElasticsearchMigrationRunner\" \"-Dexec.args=--execute --payments-only\"");
            System.out.println("================================================================================\n");
            return;
        }

        try {
            if (executeMode) {
                if (paymentsOnly) {
                    runPaymentOnlyMigration();
                } else {
                    runLiveMigration();
                }
            } else {
                runDryRun();
            }
        } finally {
            Neo4jConnection.closeDriver();
            if (executeMode) {
                ElasticsearchConnection.closeClient();
            }
        }
    }
}
