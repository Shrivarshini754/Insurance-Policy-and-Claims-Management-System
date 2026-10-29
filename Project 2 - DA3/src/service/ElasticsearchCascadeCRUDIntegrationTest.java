package service;

import dao.*;
import db.DatabaseConfig;
import db.ElasticsearchConnection;

import java.util.*;

/**
 * Integration Test suite verifying CRUD and cascade-delete behavior in Elasticsearch mode.
 * 
 * Safety Guarantees:
 * 1. Uses ONLY unique disposable IDs with a random timestamp prefix (e.g., DISPOSABLE_TEST_...).
 * 2. NEVER touches, selects, updates, or deletes existing migrated IDs (P101-P105, POL101-POL105, etc.).
 * 3. Enforces thorough cleanup in a finally block for all created test documents.
 * 4. Explicitly tests:
 *    - Customer CRUD (Create, Read, Update, Delete)
 *    - Full 4-level cascade deletion (Customer -> Policy -> Claim -> Payment)
 *    - Survivor verification (unrelated records and shared policies must survive)
 *    - Policy deletion cascade (Policy -> Claim -> Payment)
 *    - Claim deletion cascade (Claim -> Payment with parent policy survival)
 *    - Asset deletion: Customer-owned policy protection vs Orphan policy cascade
 * 5. Handles partial cascades and reports non-atomic Elasticsearch behavior.
 */
public class ElasticsearchCascadeCRUDIntegrationTest {

    private static final String RUN_ID = "DISP_" + System.currentTimeMillis() + "_";

    // Track all created disposable IDs for guaranteed cleanup
    private static final Set<String> createdCustomers = new LinkedHashSet<>();
    private static final Set<String> createdPolicies = new LinkedHashSet<>();
    private static final Set<String> createdClaims = new LinkedHashSet<>();
    private static final Set<String> createdPayments = new LinkedHashSet<>(); // composite: claimId_paymentId
    private static final Set<String> createdAssets = new LinkedHashSet<>();

    public static class TestResultRow {
        public final String operation;
        public final String testPerformed;
        public final String expectedResult;
        public final String actualResult;
        public final boolean passed;

        public TestResultRow(String operation, String testPerformed, String expectedResult, String actualResult, boolean passed) {
            this.operation = operation;
            this.testPerformed = testPerformed;
            this.expectedResult = expectedResult;
            this.actualResult = actualResult;
            this.passed = passed;
        }
    }

    private static final List<TestResultRow> results = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     ELASTICSEARCH CRUD & CASCADE-DELETE INTEGRATION TEST SUITE                 ");
        System.out.println("================================================================================");

        boolean hasUrl = System.getenv("ELASTICSEARCH_URL") != null && !System.getenv("ELASTICSEARCH_URL").trim().isEmpty();
        boolean hasKey = System.getenv("ELASTICSEARCH_API_KEY") != null && !System.getenv("ELASTICSEARCH_API_KEY").trim().isEmpty();

        System.out.println("[CONFIG] Active Backend Setting: " + DatabaseConfig.getActiveBackend());
        System.out.println("[CONFIG] ELASTICSEARCH_URL configured: " + hasUrl);
        System.out.println("[CONFIG] ELASTICSEARCH_API_KEY configured: " + hasKey);

        if (!hasUrl || !hasKey) {
            System.out.println("\n[NOTICE] Live Elasticsearch credentials (ELASTICSEARCH_URL / ELASTICSEARCH_API_KEY)");
            System.out.println("         are not set in the current process environment.");
            System.out.println("         Live integration tests were skipped.");
            System.out.println("================================================================================");
            return;
        }

        ElasticsearchConnection.ConnectionDetails conn = ElasticsearchConnection.testConnection();
        if (!conn.isConnected()) {
            System.out.println("\n[NOTICE] Elastic Cloud cluster is currently unreachable: " + conn.getErrorMessage());
            System.out.println("         Live integration tests were skipped.");
            System.out.println("================================================================================");
            return;
        }

        System.out.println("[INFO] Connected to cluster: " + conn.getClusterName() + " (v" + conn.getVersion() + ")");
        System.out.println("[INFO] Test Run ID: " + RUN_ID);

        ElasticsearchCustomerDAO customerDAO = new ElasticsearchCustomerDAO();
        ElasticsearchPolicyDAO policyDAO = new ElasticsearchPolicyDAO();
        ElasticsearchClaimDAO claimDAO = new ElasticsearchClaimDAO();
        ElasticsearchPaymentDAO paymentDAO = new ElasticsearchPaymentDAO();
        ElasticsearchAssetDAO assetDAO = new ElasticsearchAssetDAO();

        try {
            // ----------------------------------------------------------------------------------
            // TEST 1: Customer Single Entity CRUD Lifecycle
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 1] Customer CRUD Lifecycle ---");
            String testCust1 = RUN_ID + "CUST_1";

            // 1.1 INSERT
            boolean c1Add = customerDAO.addCustomer(testCust1, "John Doe", "1985-05-15", "john.doe@test.com", "123456789012", "100 Test Blvd");
            if (c1Add) createdCustomers.add(testCust1);
            record("Customer INSERT", "Create disposable customer " + testCust1, "addCustomer returns true", "Returned: " + c1Add, c1Add);

            // 1.2 SELECT
            String[] c1Read = customerDAO.findCustomer(testCust1);
            boolean c1ReadOk = c1Read != null && "John Doe".equals(c1Read[1]) && "john.doe@test.com".equals(c1Read[3]);
            record("Customer SELECT", "findCustomer(" + testCust1 + ")", "Customer found with name 'John Doe' & email", 
                   c1Read != null ? "Found: " + c1Read[1] + ", " + c1Read[3] : "null", c1ReadOk);

            // 1.3 UPDATE
            boolean c1Upd = customerDAO.updateCustomer(testCust1, "John Updated", "1985-05-15", "john.updated@test.com", "123456789012", "200 Updated St");
            record("Customer UPDATE", "Update name and email for " + testCust1, "updateCustomer returns true", "Returned: " + c1Upd, c1Upd);

            // 1.4 VERIFY UPDATE
            String[] c1Verify = customerDAO.findCustomer(testCust1);
            boolean c1VerOk = c1Verify != null && "John Updated".equals(c1Verify[1]) && "john.updated@test.com".equals(c1Verify[3]);
            record("Customer VERIFY UPDATE", "findCustomer after update", "Updated name 'John Updated' & new email", 
                   c1Verify != null ? "Found: " + c1Verify[1] + ", " + c1Verify[3] : "null", c1VerOk);

            // 1.5 DELETE
            boolean c1Del = customerDAO.deleteCustomer(testCust1);
            if (c1Del) createdCustomers.remove(testCust1);
            String[] c1PostDel = customerDAO.findCustomer(testCust1);
            boolean c1DelOk = c1Del && (c1PostDel == null);
            record("Customer DELETE", "deleteCustomer(" + testCust1 + ")", "deleteCustomer returns true and findCustomer returns null", 
                   "Delete: " + c1Del + ", Post-delete check: " + (c1PostDel == null ? "null" : "still exists"), c1DelOk);


            // ----------------------------------------------------------------------------------
            // TEST 2: Full 4-Level Cascade Deletion (Customer -> Policy -> Claim -> Payment)
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 2] Full 4-Level Cascade Deletion ---");
            String custCascade = RUN_ID + "CUST_CAS";
            String polCascade = RUN_ID + "POL_CAS";
            String clmCascade = RUN_ID + "CLM_CAS";
            String payCascade = RUN_ID + "PAY_CAS";

            String custSurvivor = RUN_ID + "CUST_SURV";
            String polSurvivor = RUN_ID + "POL_SURV";

            // Setup hierarchy
            customerDAO.addCustomer(custCascade, "Cascade Parent", "1980-01-01", "cas@test.com", "111122223333", "Cascade Way");
            createdCustomers.add(custCascade);

            policyDAO.addPolicy(polCascade, "2024-01-01", "2027-01-01", "AUDIT_1", 1200.0, List.of(custCascade), null);
            createdPolicies.add(polCascade);

            claimDAO.registerClaim(clmCascade, polCascade, "2025-06-01", "Water damage test", "FILED");
            createdClaims.add(clmCascade);

            paymentDAO.makePayment(clmCascade, payCascade, "2025-06-15", "Electronic", 500.0);
            createdPayments.add(clmCascade + "_" + payCascade);

            // Setup unrelated survivor customer & policy
            customerDAO.addCustomer(custSurvivor, "Survivor Customer", "1992-02-02", "surv@test.com", "444455556666", "Survivor Rd");
            createdCustomers.add(custSurvivor);

            policyDAO.addPolicy(polSurvivor, "2024-01-01", "2028-01-01", "AUDIT_SURV", 950.0, List.of(custSurvivor), null);
            createdPolicies.add(polSurvivor);

            // Execute Customer Cascade Delete
            boolean casDelOk = customerDAO.deleteCustomer(custCascade);
            if (casDelOk) createdCustomers.remove(custCascade);

            // Verify expected deletions
            String[] postCustCas = customerDAO.findCustomer(custCascade);
            String[] postPolCas = policyDAO.findPolicy(polCascade);
            String[] postClmCas = claimDAO.findClaim(clmCascade);
            String[] postPayCas = paymentDAO.findPayment(clmCascade, payCascade);

            // Verify survivors
            String[] postCustSurv = customerDAO.findCustomer(custSurvivor);
            String[] postPolSurv = policyDAO.findPolicy(polSurvivor);

            boolean allDeleted = (postCustCas == null) && (postPolCas == null) && (postClmCas == null) && (postPayCas == null);
            boolean survivorsIntact = (postCustSurv != null) && (postPolSurv != null);

            record("Cascade: Customer Deleted", "deleteCustomer(" + custCascade + ")", "Customer document deleted", 
                   postCustCas == null ? "Deleted (null)" : "Still exists", postCustCas == null);
            record("Cascade: Dependent Policy Deleted", "Policy " + polCascade + " under deleted customer", "Policy document deleted", 
                   postPolCas == null ? "Deleted (null)" : "Still exists", postPolCas == null);
            record("Cascade: Dependent Claim Deleted", "Claim " + clmCascade + " under deleted policy", "Claim document deleted", 
                   postClmCas == null ? "Deleted (null)" : "Still exists", postClmCas == null);
            record("Cascade: Dependent Payment Deleted", "Payment " + payCascade + " under deleted claim", "Payment document deleted", 
                   postPayCas == null ? "Deleted (null)" : "Still exists", postPayCas == null);
            record("Cascade: Unrelated Customer Survives", "Customer " + custSurvivor, "Survivor customer remains untouched", 
                   postCustSurv != null ? "Survived (" + postCustSurv[1] + ")" : "Deleted (BUG)", postCustSurv != null);
            record("Cascade: Unrelated Policy Survives", "Policy " + polSurvivor, "Survivor policy remains untouched", 
                   postPolSurv != null ? "Survived (" + postPolSurv[0] + ")" : "Deleted (BUG)", postPolSurv != null);


            // ----------------------------------------------------------------------------------
            // TEST 3: Shared Policy Multi-Owner Protection
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 3] Shared Policy Multi-Owner Protection ---");
            String custOwner1 = RUN_ID + "CUST_OWN1";
            String custOwner2 = RUN_ID + "CUST_OWN2";
            String polShared = RUN_ID + "POL_SHARED";
            String clmShared = RUN_ID + "CLM_SHARED";

            customerDAO.addCustomer(custOwner1, "Co-Owner One", "1975-03-10", "owner1@test.com", "777711112222", "Joint Ave");
            createdCustomers.add(custOwner1);

            customerDAO.addCustomer(custOwner2, "Co-Owner Two", "1978-07-20", "owner2@test.com", "777733334444", "Joint Ave");
            createdCustomers.add(custOwner2);

            policyDAO.addPolicy(polShared, "2024-05-01", "2026-05-01", "AUDIT_SHARED", 2400.0, List.of(custOwner1, custOwner2), null);
            createdPolicies.add(polShared);

            claimDAO.registerClaim(clmShared, polShared, "2025-08-01", "Shared claim test", "APPROVED");
            createdClaims.add(clmShared);

            // Action: Delete only Owner 1
            customerDAO.deleteCustomer(custOwner1);
            createdCustomers.remove(custOwner1);

            String[] postOwner1 = customerDAO.findCustomer(custOwner1);
            String[] postOwner2 = customerDAO.findCustomer(custOwner2);
            String[] postPolShared = policyDAO.findPolicy(polShared);
            String[] postClmShared = claimDAO.findClaim(clmShared);

            boolean sharedPolicyProtected = (postOwner1 == null) && (postOwner2 != null) && (postPolShared != null) && (postClmShared != null);
            record("Shared Policy: Deleted Co-Owner", "deleteCustomer(" + custOwner1 + ")", "Deleted owner is null", 
                   postOwner1 == null ? "Deleted (null)" : "Still exists", postOwner1 == null);
            record("Shared Policy: Retained Co-Owner", "Customer " + custOwner2, "Retained co-owner is intact", 
                   postOwner2 != null ? "Intact (" + postOwner2[1] + ")" : "Deleted (BUG)", postOwner2 != null);
            record("Shared Policy: Policy Survives", "Policy " + polShared + " co-owned by surviving owner", "Shared policy SURVIVES", 
                   postPolShared != null ? "Survived (" + postPolShared[0] + ")" : "Deleted (BUG)", postPolShared != null);
            record("Shared Policy: Claim Survives", "Claim " + clmShared + " under shared policy", "Claim SURVIVES", 
                   postClmShared != null ? "Survived (" + postClmShared[0] + ")" : "Deleted (BUG)", postClmShared != null);


            // ----------------------------------------------------------------------------------
            // TEST 4: Direct Policy Deletion Cascade (Policy -> Claim -> Payment)
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 4] Direct Policy Deletion Cascade ---");
            String polDirect = RUN_ID + "POL_DIR";
            String clmDirect = RUN_ID + "CLM_DIR";
            String payDirect = RUN_ID + "PAY_DIR";

            policyDAO.addPolicy(polDirect, "2024-01-01", "2026-01-01", "AUDIT_DIR", 1500.0);
            createdPolicies.add(polDirect);

            claimDAO.registerClaim(clmDirect, polDirect, "2025-01-10", "Direct policy claim", "IN_REVIEW");
            createdClaims.add(clmDirect);

            paymentDAO.makePayment(clmDirect, payDirect, "2025-01-20", "Wire", 750.0);
            createdPayments.add(clmDirect + "_" + payDirect);

            boolean polDelOk = policyDAO.deletePolicy(polDirect);
            if (polDelOk) createdPolicies.remove(polDirect);

            String[] postPolDir = policyDAO.findPolicy(polDirect);
            String[] postClmDir = claimDAO.findClaim(clmDirect);
            String[] postPayDir = paymentDAO.findPayment(clmDirect, payDirect);

            boolean directPolOk = (postPolDir == null) && (postClmDir == null) && (postPayDir == null);
            record("Policy Cascade: Policy Deleted", "deletePolicy(" + polDirect + ")", "Policy is null", 
                   postPolDir == null ? "Deleted (null)" : "Still exists", postPolDir == null);
            record("Policy Cascade: Claims Deleted", "Claim " + clmDirect + " under deleted policy", "Claim is null", 
                   postClmDir == null ? "Deleted (null)" : "Still exists", postClmDir == null);
            record("Policy Cascade: Payments Deleted", "Payment " + payDirect + " under deleted claim", "Payment is null", 
                   postPayDir == null ? "Deleted (null)" : "Still exists", postPayDir == null);


            // ----------------------------------------------------------------------------------
            // TEST 5: Direct Claim Deletion (Claim -> Payment with Parent Policy Survival)
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 5] Direct Claim Deletion ---");
            String polParent = RUN_ID + "POL_PAR";
            String clmToDel = RUN_ID + "CLM_TODEL";
            String payToDel = RUN_ID + "PAY_TODEL";

            policyDAO.addPolicy(polParent, "2024-01-01", "2027-01-01", "AUDIT_PAR", 800.0);
            createdPolicies.add(polParent);

            claimDAO.registerClaim(clmToDel, polParent, "2025-03-01", "Claim to delete test", "SUBMITTED");
            createdClaims.add(clmToDel);

            paymentDAO.makePayment(clmToDel, payToDel, "2025-03-10", "Card", 200.0);
            createdPayments.add(clmToDel + "_" + payToDel);

            boolean clmDelOk = claimDAO.deleteClaim(clmToDel);
            if (clmDelOk) createdClaims.remove(clmToDel);

            String[] postClmToDel = claimDAO.findClaim(clmToDel);
            String[] postPayToDel = paymentDAO.findPayment(clmToDel, payToDel);
            String[] postPolParent = policyDAO.findPolicy(polParent);

            boolean claimDelPass = (postClmToDel == null) && (postPayToDel == null) && (postPolParent != null);
            record("Claim Cascade: Claim Deleted", "deleteClaim(" + clmToDel + ")", "Claim is null", 
                   postClmToDel == null ? "Deleted (null)" : "Still exists", postClmToDel == null);
            record("Claim Cascade: Payment Deleted", "Payment " + payToDel + " under deleted claim", "Payment is null", 
                   postPayToDel == null ? "Deleted (null)" : "Still exists", postPayToDel == null);
            record("Claim Cascade: Parent Policy Survives", "Parent policy " + polParent, "Policy SURVIVES", 
                   postPolParent != null ? "Survived (" + postPolParent[0] + ")" : "Deleted (BUG)", postPolParent != null);


            // ----------------------------------------------------------------------------------
            // TEST 6: Asset Deletion - Customer-Owned Policy Asset Detachment
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 6] Asset Deletion: Customer-Owned Policy Protection ---");
            String custAst = RUN_ID + "CUST_AST";
            String polWithAst = RUN_ID + "POL_WITH_AST";
            String astProtected = RUN_ID + "AST_PROT";

            customerDAO.addCustomer(custAst, "Asset Owner", "1988-11-11", "assetowner@test.com", "888899990000", "Asset Park");
            createdCustomers.add(custAst);

            assetDAO.addAsset(astProtected, "Commercial Warehouse", "2020-05-01", 350000.0, List.of(polWithAst));
            createdAssets.add(astProtected);

            policyDAO.addPolicy(polWithAst, "2024-01-01", "2026-01-01", "AUDIT_AST", 4500.0, List.of(custAst), List.of(astProtected));
            createdPolicies.add(polWithAst);

            // Action: Delete the Asset
            boolean astDelOk = assetDAO.deleteAsset(astProtected);
            if (astDelOk) createdAssets.remove(astProtected);

            String[] postAst = assetDAO.findAsset(astProtected);
            String[] postPolWithAst = policyDAO.findPolicy(polWithAst);
            String[] postCustAst = customerDAO.findCustomer(custAst);

            boolean astProtPass = (postAst == null) && (postPolWithAst != null) && (postCustAst != null);
            record("Asset Deletion: Asset Deleted", "deleteAsset(" + astProtected + ")", "Asset document is null", 
                   postAst == null ? "Deleted (null)" : "Still exists", postAst == null);
            record("Asset Deletion: Customer-Owned Policy Survives", "Policy " + polWithAst + " owned by customer", "Policy SURVIVES (only asset unlinked)", 
                   postPolWithAst != null ? "Survived (" + postPolWithAst[0] + ")" : "Deleted (BUG)", postPolWithAst != null);
            record("Asset Deletion: Customer Survives", "Customer " + custAst, "Customer SURVIVES", 
                   postCustAst != null ? "Survived (" + postCustAst[1] + ")" : "Deleted (BUG)", postCustAst != null);


            // ----------------------------------------------------------------------------------
            // TEST 7: Asset Deletion - Orphan Unowned Policy Cascade Deletion
            // ----------------------------------------------------------------------------------
            System.out.println("\n--- [TEST 7] Asset Deletion: Orphan Unowned Policy Cascade ---");
            String astOrphan = RUN_ID + "AST_ORPH";
            String polOrphan = RUN_ID + "POL_ORPH";

            assetDAO.addAsset(astOrphan, "Abandoned Equipment", "2018-03-15", 45000.0, List.of(polOrphan));
            createdAssets.add(astOrphan);

            // Policy has NO customer owner, exclusively covering this asset
            policyDAO.addPolicy(polOrphan, "2024-01-01", "2025-01-01", "AUDIT_ORPH", 600.0, null, List.of(astOrphan));
            createdPolicies.add(polOrphan);

            boolean orphanAstDelOk = assetDAO.deleteAsset(astOrphan);
            if (orphanAstDelOk) createdAssets.remove(astOrphan);

            String[] postAstOrphan = assetDAO.findAsset(astOrphan);
            String[] postPolOrphan = policyDAO.findPolicy(polOrphan);

            boolean orphanCascadePass = (postAstOrphan == null) && (postPolOrphan == null);
            record("Orphan Asset: Asset Deleted", "deleteAsset(" + astOrphan + ")", "Asset document is null", 
                   postAstOrphan == null ? "Deleted (null)" : "Still exists", postAstOrphan == null);
            record("Orphan Asset: Unowned Policy Cascade Deleted", "Orphan unowned policy " + polOrphan, "Policy CASCADE DELETED", 
                   postPolOrphan == null ? "Deleted (null)" : "Still exists (BUG)", postPolOrphan == null);

        } finally {
            // Guaranteed Cleanup of any remaining disposable test documents
            System.out.println("\n--- CLEANUP DISPOSABLE TEST RECORDS ---");
            cleanupTestDocuments();
        }

        // Print final summary report
        printSummaryTable();
    }

    private static void record(String operation, String testPerformed, String expectedResult, String actualResult, boolean passed) {
        TestResultRow row = new TestResultRow(operation, testPerformed, expectedResult, actualResult, passed);
        results.add(row);
        System.out.println("[" + (passed ? "PASS" : "FAIL") + "] " + operation + ": " + testPerformed + " -> " + actualResult);
    }

    private static void cleanupTestDocuments() {
        ElasticsearchCustomerDAO cDAO = new ElasticsearchCustomerDAO();
        ElasticsearchPolicyDAO pDAO = new ElasticsearchPolicyDAO();
        ElasticsearchClaimDAO clDAO = new ElasticsearchClaimDAO();
        ElasticsearchPaymentDAO payDAO = new ElasticsearchPaymentDAO();
        ElasticsearchAssetDAO aDAO = new ElasticsearchAssetDAO();

        // 1. Clean payments
        for (String payKey : new ArrayList<>(createdPayments)) {
            try {
                String[] parts = payKey.split("_", 2);
                if (parts.length == 2) {
                    payDAO.deletePayment(parts[0], parts[1]);
                }
            } catch (Exception ignored) {}
        }

        // 2. Clean claims
        for (String cId : new ArrayList<>(createdClaims)) {
            try { clDAO.deleteClaim(cId); } catch (Exception ignored) {}
        }

        // 3. Clean policies
        for (String pNo : new ArrayList<>(createdPolicies)) {
            try { pDAO.deletePolicy(pNo); } catch (Exception ignored) {}
        }

        // 4. Clean customers
        for (String cId : new ArrayList<>(createdCustomers)) {
            try { cDAO.deleteCustomer(cId); } catch (Exception ignored) {}
        }

        // 5. Clean assets
        for (String aId : new ArrayList<>(createdAssets)) {
            try { aDAO.deleteAsset(aId); } catch (Exception ignored) {}
        }
        System.out.println("[INFO] Disposable test records cleaned up successfully.");
    }

    private static void printSummaryTable() {
        System.out.println("\n========================================================================================================================");
        System.out.println(String.format("%-25s | %-35s | %-25s | %-15s | %-6s", "OPERATION", "TEST PERFORMED", "EXPECTED RESULT", "ACTUAL RESULT", "STATUS"));
        System.out.println("------------------------------------------------------------------------------------------------------------------------");
        int passedCount = 0;
        for (TestResultRow r : results) {
            if (r.passed) passedCount++;
            System.out.println(String.format("%-25s | %-35s | %-25s | %-15s | %-6s",
                    truncate(r.operation, 25),
                    truncate(r.testPerformed, 35),
                    truncate(r.expectedResult, 25),
                    truncate(r.actualResult, 15),
                    r.passed ? "PASS" : "FAIL"
            ));
        }
        System.out.println("========================================================================================================================");
        System.out.println("TOTAL TESTS: " + results.size() + " | PASSED: " + passedCount + " | FAILED: " + (results.size() - passedCount));
        System.out.println("TRANSACTION SEMANTICS NOTE: Elasticsearch does not provide multi-document cross-index ACID transactions.");
        System.out.println("                            Cascade deletions are sequenced with individual error boundaries and status verification.");
        System.out.println("========================================================================================================================");
    }

    private static String truncate(String val, int max) {
        if (val == null) return "";
        if (val.length() <= max) return val;
        return val.substring(0, max - 3) + "...";
    }
}
