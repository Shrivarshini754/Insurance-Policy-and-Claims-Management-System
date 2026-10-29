package dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import db.ElasticsearchConnection;
import service.ElasticsearchIndexSetup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch-backed DAO for Policy entities.
 * Directly queries and mutates the 'insurance_policies' index.
 * Preserves the exact public API of PolicyDAO for seamless Swing UI integration.
 */
public class ElasticsearchPolicyDAO extends PolicyDAO {

    private final AbstractElasticsearchDAO helper = new AbstractElasticsearchDAO() {};

    @Override
    public List<String[]> displayPolicies() {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> resp = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .query(q -> q.matchAll(m -> m))
                .size(1000)
                .sort(so -> so.field(f -> f.field("policyNo").order(SortOrder.Asc))),
                Map.class
            );

            for (Hit<Map> hit : resp.hits().hits()) {
                Map<String, Object> doc = hit.source();
                if (doc != null) {
                    results.add(new String[] {
                        helper.asString(doc.get("policyNo")),
                        helper.asString(doc.get("startDate")),
                        helper.asString(doc.get("expiryDate")),
                        helper.asString(doc.get("auditId")),
                        String.valueOf(helper.asDouble(doc.get("premiumAmount"), 0.0))
                    });
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("Elasticsearch displayPolicies error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return results;
        }
    }

    @Override
    public String[] findPolicy(String policyNo) {
        if (policyNo == null || policyNo.trim().isEmpty()) return null;
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            GetResponse<Map> resp = client.get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .id(policyNo.trim()),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                Map<String, Object> doc = resp.source();
                return new String[] {
                    helper.asString(doc.get("policyNo")),
                    helper.asString(doc.get("startDate")),
                    helper.asString(doc.get("expiryDate")),
                    helper.asString(doc.get("auditId")),
                    String.valueOf(helper.asDouble(doc.get("premiumAmount"), 0.0))
                };
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch findPolicy error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return null;
    }

    @Override
    public boolean addPolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {
        return addPolicy(policyNo, startDate, expiryDate, auditId, premiumAmount, null, null);
    }

    @SuppressWarnings("unchecked")
    public boolean addPolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount,
            List<String> customerIds,
            List<String> assetIds) {

        if (policyNo == null || policyNo.trim().isEmpty()) return false;
        String id = policyNo.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(id)).value();
            if (exists) return false;

            List<String> cIds = (customerIds != null) ? new ArrayList<>(customerIds) : new ArrayList<>();
            List<String> aIds = (assetIds != null) ? new ArrayList<>(assetIds) : new ArrayList<>();

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("policyNo", id);
            if (startDate != null && !startDate.trim().isEmpty()) doc.put("startDate", startDate.trim());
            if (expiryDate != null && !expiryDate.trim().isEmpty()) doc.put("expiryDate", expiryDate.trim());
            doc.put("auditId", helper.asString(auditId));
            doc.put("premiumAmount", premiumAmount);
            doc.put("customerIds", cIds);
            doc.put("assetIds", aIds);
            doc.put("claimIds", new ArrayList<String>());

            client.index(i -> i
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .id(id)
                .document(doc)
            );

            // Synchronize with referenced customers
            for (String cId : cIds) {
                if (cId != null && !cId.trim().isEmpty() && client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(cId.trim())).value()) {
                    GetResponse<Map> cResp = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(cId.trim()), Map.class);
                    if (cResp.found() && cResp.source() != null) {
                        List<String> pList = helper.asStringList(cResp.source().get("policyIds"));
                        if (!pList.contains(id)) {
                            pList.add(id);
                            client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(cId.trim()).doc(Map.of("policyIds", pList)), Map.class);
                        }
                    }
                }
            }

            // Synchronize with referenced assets
            for (String aId : aIds) {
                if (aId != null && !aId.trim().isEmpty() && client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(aId.trim())).value()) {
                    GetResponse<Map> aResp = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(aId.trim()), Map.class);
                    if (aResp.found() && aResp.source() != null) {
                        List<String> pList = helper.asStringList(aResp.source().get("policyIds"));
                        if (!pList.contains(id)) {
                            pList.add(id);
                            client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(aId.trim()).doc(Map.of("policyIds", pList)), Map.class);
                        }
                    }
                }
            }

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            if (!cIds.isEmpty()) helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
            if (!aIds.isEmpty()) helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch addPolicy error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean updatePolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        if (policyNo == null || policyNo.trim().isEmpty()) return false;
        String id = policyNo.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(id)).value();
            if (!exists) return false;

            Map<String, Object> updates = new LinkedHashMap<>();
            updates.put("policyNo", id);
            if (startDate != null && !startDate.trim().isEmpty()) updates.put("startDate", startDate.trim());
            if (expiryDate != null && !expiryDate.trim().isEmpty()) updates.put("expiryDate", expiryDate.trim());
            updates.put("auditId", helper.asString(auditId));
            updates.put("premiumAmount", premiumAmount);

            client.update(u -> u
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .id(id)
                .doc(updates),
                Map.class
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch updatePolicy error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deletePolicy(String policyNo) {
        if (policyNo == null || policyNo.trim().isEmpty()) return false;
        String id = policyNo.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(id)).value();
            if (!exists) return false;

            // Delete claims under this policy and their payments
            SearchResponse<Map> clmSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .query(q -> q.term(t -> t.field("policyNo").value(id)))
                .size(500),
                Map.class
            );

            for (Hit<Map> clmHit : clmSearch.hits().hits()) {
                Map<String, Object> clmDoc = clmHit.source();
                if (clmDoc != null) {
                    String claimId = helper.asString(clmDoc.get("claimId"));
                    // Delete individual payment documents if tracked
                    List<String> payIds = helper.asStringList(clmDoc.get("paymentIds"));
                    for (String pId : payIds) {
                        try {
                            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(claimId + "_" + pId));
                        } catch (Exception ignored) {}
                    }
                    // Sweep delete any payments with claimId
                    try {
                        client.deleteByQuery(d -> d
                            .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                            .refresh(true)
                            .query(q -> q.term(t -> t.field("claimId").value(claimId)))
                        );
                    } catch (Exception ignored) {}
                    client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(claimId));
                }
            }

            // Remove policy reference from customers
            SearchResponse<Map> custSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                .query(q -> q.term(t -> t.field("policyIds").value(id)))
                .size(500),
                Map.class
            );
            for (Hit<Map> custHit : custSearch.hits().hits()) {
                Map<String, Object> cDoc = custHit.source();
                if (cDoc != null) {
                    String pId = helper.asString(cDoc.get("personId"));
                    List<String> pols = helper.asStringList(cDoc.get("policyIds"));
                    pols.remove(id);
                    client.update(u -> u
                        .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                        .id(pId)
                        .doc(Map.of("policyIds", pols)),
                        Map.class
                    );
                }
            }

            // Remove policy reference from assets
            SearchResponse<Map> assetSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                .query(q -> q.term(t -> t.field("policyIds").value(id)))
                .size(500),
                Map.class
            );
            for (Hit<Map> aHit : assetSearch.hits().hits()) {
                Map<String, Object> aDoc = aHit.source();
                if (aDoc != null) {
                    String aId = aHit.id();
                    List<String> pols = helper.asStringList(aDoc.get("policyIds"));
                    if (pols.remove(id)) {
                        client.update(u -> u
                            .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                            .id(aId)
                            .doc(Map.of("policyIds", pols)),
                            Map.class
                        );
                    }
                }
            }

            // Delete policy document
            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(id));
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch deletePolicy error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public int getPolicyCount() {
        try {
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c.index(ElasticsearchIndexSetup.INDEX_POLICIES));
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getPolicyCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    public int getActivePolicyCount() {
        try {
            String today = LocalDate.now().toString();
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .query(q -> q.range(r -> r.date(d -> d.field("expiryDate").gte(today))))
            );
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getActivePolicyCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    public double getTotalPremium() {
        try {
            SearchResponse<Void> resp = ElasticsearchConnection.getClient().search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .size(0)
                .aggregations("totalPremium", a -> a.sum(sm -> sm.field("premiumAmount"))),
                Void.class
            );

            Aggregate agg = resp.aggregations().get("totalPremium");
            if (agg != null && agg.isSum()) {
                return agg.sum().value();
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch getTotalPremium error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return 0.0;
    }
}
