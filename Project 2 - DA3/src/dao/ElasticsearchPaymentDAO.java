package dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import db.ElasticsearchConnection;
import service.ElasticsearchIndexSetup;

import java.util.*;

/**
 * ElasticsearchPaymentDAO - Elasticsearch implementation of PaymentDAO.
 * Preserves all public method signatures, composite document ID handling (<claimId>_<paymentId>),
 * parent Claim synchronization, and immediate index refreshing.
 */
public class ElasticsearchPaymentDAO extends PaymentDAO {

    private final AbstractElasticsearchDAO helper = new AbstractElasticsearchDAO() {};

    @Override
    @SuppressWarnings("unchecked")
    public List<String[]> displayPayments() {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> response = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                .size(10000)
                .query(q -> q.matchAll(m -> m)),
                Map.class
            );

            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> src = hit.source();
                if (src == null) continue;

                String claimId = helper.asString(src.get("claimId"));
                String paymentId = helper.asString(src.get("paymentId"));
                String paymentDate = helper.asString(src.get("paymentDate"));
                String paymentMethod = helper.asString(src.get("paymentMethod"));
                double amountPaid = helper.asDouble(src.get("amountPaid"));

                results.add(new String[] {
                    claimId,
                    paymentId,
                    paymentDate,
                    paymentMethod,
                    String.valueOf(amountPaid)
                });
            }

            results.sort(Comparator.comparing((String[] row) -> row[0]).thenComparing(row -> row[1]));
        } catch (Exception e) {
            System.err.println("Elasticsearch displayPayments error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return results;
    }

    @Override
    public boolean makePayment(String claimId, String paymentId, String date, String method) {
        return makePayment(claimId, paymentId, date, method, 0.0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean makePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            System.err.println("Claim ID and Payment ID are required.");
            return false;
        }

        String cId = claimId.trim();
        String pId = paymentId.trim();
        String docId = cId + "_" + pId;

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            // 1. Check composite primary key does not exist
            boolean payExists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(docId)).value();
            if (payExists) {
                System.err.println("Payment with ID '" + pId + "' for Claim '" + cId + "' already exists.");
                return false;
            }

            // 2. Check referenced Claim exists
            boolean claimExists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId)).value();
            if (!claimExists) {
                System.err.println("Referenced claim '" + cId + "' does not exist. Cannot record payment.");
                return false;
            }

            // 3. Index payment document (exclude _id and compositeId from document body)
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("claimId", cId);
            doc.put("paymentId", pId);
            doc.put("paymentDate", date != null ? date.trim() : "");
            doc.put("paymentMethod", method != null ? method.trim() : "");
            doc.put("amountPaid", amountPaid);

            client.index(i -> i
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                .id(docId)
                .document(doc)
            );

            // 4. Synchronize paymentId with parent Claim
            GetResponse<Map> claimGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId), Map.class);
            if (claimGet.found() && claimGet.source() != null) {
                Map<String, Object> claimSrc = claimGet.source();
                List<String> payIds = helper.asStringList(claimSrc.get("paymentIds"));
                if (!payIds.contains(pId)) {
                    payIds.add(pId);
                    Map<String, Object> clmUpdate = new LinkedHashMap<>();
                    clmUpdate.put("paymentIds", payIds);
                    client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId).doc(clmUpdate), Map.class);
                    helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
                }
            }

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch makePayment error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean updatePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            return false;
        }

        String cId = claimId.trim();
        String pId = paymentId.trim();
        String docId = cId + "_" + pId;

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            // 1. Check claim exists
            boolean claimExists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId)).value();
            if (!claimExists) {
                System.err.println("Referenced claim '" + cId + "' does not exist. Cannot update payment.");
                return false;
            }

            // 2. Check payment exists
            boolean payExists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(docId)).value();
            if (!payExists) {
                return false;
            }

            Map<String, Object> updates = new LinkedHashMap<>();
            updates.put("claimId", cId);
            updates.put("paymentId", pId);
            updates.put("paymentDate", date != null ? date.trim() : "");
            updates.put("paymentMethod", method != null ? method.trim() : "");
            updates.put("amountPaid", amountPaid);

            client.update(u -> u
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                .id(docId)
                .doc(updates),
                Map.class
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch updatePayment error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deletePayment(String claimId, String paymentId) {
        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            return false;
        }

        String cId = claimId.trim();
        String pId = paymentId.trim();
        String docId = cId + "_" + pId;

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(docId)).value();
            if (!exists) {
                System.err.println("Payment '" + pId + "' for Claim '" + cId + "' does not exist.");
                return false;
            }

            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(docId));

            // Remove from parent claim's paymentIds
            GetResponse<Map> claimGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId), Map.class);
            if (claimGet.found() && claimGet.source() != null) {
                Map<String, Object> claimSrc = claimGet.source();
                List<String> payIds = helper.asStringList(claimSrc.get("paymentIds"));
                if (payIds.remove(pId)) {
                    Map<String, Object> clmUpdate = new LinkedHashMap<>();
                    clmUpdate.put("paymentIds", payIds);
                    client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(cId).doc(clmUpdate), Map.class);
                    helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
                }
            }

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch deletePayment error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public int getPaymentCount() {
        try {
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
            );
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getPaymentCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    public double getTotalAmountPaid() {
        try {
            SearchResponse<Void> resp = ElasticsearchConnection.getClient().search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                .size(0)
                .aggregations("totalAmountPaid", a -> a.sum(sm -> sm.field("amountPaid"))),
                Void.class
            );

            Aggregate agg = resp.aggregations().get("totalAmountPaid");
            if (agg != null && agg.isSum()) {
                return agg.sum().value();
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch getTotalAmountPaid error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return 0.0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String[] findPayment(String claimId, String paymentId) {
        if (claimId == null || claimId.trim().isEmpty() ||
            paymentId == null || paymentId.trim().isEmpty()) {
            return null;
        }

        String docId = claimId.trim() + "_" + paymentId.trim();

        try {
            GetResponse<Map> resp = ElasticsearchConnection.getClient().get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                .id(docId),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                Map<String, Object> src = resp.source();
                return new String[] {
                    helper.asString(src.get("claimId")),
                    helper.asString(src.get("paymentId")),
                    helper.asString(src.get("paymentDate")),
                    helper.asString(src.get("paymentMethod")),
                    String.valueOf(helper.asDouble(src.get("amountPaid")))
                };
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch findPayment error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return null;
    }
}
