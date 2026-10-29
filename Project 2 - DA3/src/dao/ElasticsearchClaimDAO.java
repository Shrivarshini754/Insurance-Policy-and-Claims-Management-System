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
 * Elasticsearch-backed DAO for Claim entities.
 * Directly queries and mutates the 'insurance_claims' index.
 * Preserves the exact public API of ClaimDAO for seamless Swing UI integration.
 */
public class ElasticsearchClaimDAO extends ClaimDAO {

    private final AbstractElasticsearchDAO helper = new AbstractElasticsearchDAO() {};

    @Override
    public List<String[]> displayClaims() {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> resp = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .query(q -> q.matchAll(m -> m))
                .size(1000)
                .sort(so -> so.field(f -> f.field("claimId").order(SortOrder.Asc))),
                Map.class
            );

            for (Hit<Map> hit : resp.hits().hits()) {
                Map<String, Object> doc = hit.source();
                if (doc != null) {
                    results.add(new String[] {
                        helper.asString(doc.get("claimId")),
                        helper.asString(doc.get("policyNo")),
                        helper.asString(doc.get("incidentDate")),
                        helper.asString(doc.get("description")),
                        String.valueOf(helper.asDouble(doc.get("claimedAmount"), 0.0)),
                        helper.asString(doc.get("status"))
                    });
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("Elasticsearch displayClaims error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return results;
        }
    }

    @Override
    public String[] findClaim(String claimId) {
        if (claimId == null || claimId.trim().isEmpty()) return null;
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            GetResponse<Map> resp = client.get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .id(claimId.trim()),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                Map<String, Object> doc = resp.source();
                return new String[] {
                    helper.asString(doc.get("claimId")),
                    helper.asString(doc.get("policyNo")),
                    helper.asString(doc.get("incidentDate")),
                    helper.asString(doc.get("description")),
                    String.valueOf(helper.asDouble(doc.get("claimedAmount"), 0.0)),
                    helper.asString(doc.get("status"))
                };
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch findClaim error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean registerClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            String status) {

        if (claimId == null || claimId.trim().isEmpty()) return false;
        String id = claimId.trim();
        String polNo = helper.asString(policyNo);

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(id)).value();
            if (exists) return false;

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("claimId", id);
            doc.put("policyNo", polNo);
            if (incidentDate != null && !incidentDate.trim().isEmpty()) doc.put("incidentDate", incidentDate.trim());
            doc.put("description", helper.asString(description));
            doc.put("claimedAmount", 0.0);
            doc.put("status", helper.asString(status));
            doc.put("paymentIds", new ArrayList<String>());
            doc.put("providerIds", new ArrayList<String>());

            client.index(i -> i
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .id(id)
                .document(doc)
            );

            // Cross-reference: link claimId into parent Policy document
            if (!polNo.isEmpty() && client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(polNo)).value()) {
                GetResponse<Map> pResp = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(polNo), Map.class);
                if (pResp.found() && pResp.source() != null) {
                    List<String> claimIds = helper.asStringList(pResp.source().get("claimIds"));
                    if (!claimIds.contains(id)) {
                        claimIds.add(id);
                        client.update(u -> u
                            .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                            .id(polNo)
                            .doc(Map.of("claimIds", claimIds)),
                            Map.class
                        );
                    }
                }
            }

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch registerClaim error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean updateClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            double claimedAmount,
            String status) {

        if (claimId == null || claimId.trim().isEmpty()) return false;
        String id = claimId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(id)).value();
            if (!exists) return false;

            Map<String, Object> updates = new LinkedHashMap<>();
            updates.put("claimId", id);
            updates.put("policyNo", helper.asString(policyNo));
            if (incidentDate != null && !incidentDate.trim().isEmpty()) updates.put("incidentDate", incidentDate.trim());
            updates.put("description", helper.asString(description));
            updates.put("claimedAmount", claimedAmount);
            updates.put("status", helper.asString(status));

            client.update(u -> u
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .id(id)
                .doc(updates),
                Map.class
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch updateClaim error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public double getClaimAmount(String claimId) {
        if (claimId == null || claimId.trim().isEmpty()) return 0.0;
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            GetResponse<Map> resp = client.get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .id(claimId.trim()),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                return helper.asDouble(resp.source().get("claimedAmount"), 0.0);
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch getClaimAmount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return 0.0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteClaim(String claimId) {
        if (claimId == null || claimId.trim().isEmpty()) return false;
        String id = claimId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(id)).value();
            if (!exists) return false;

            // Check claim document for tracked paymentIds and parent policyNo
            GetResponse<Map> clmGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(id), Map.class);
            if (clmGet.found() && clmGet.source() != null) {
                List<String> payIds = helper.asStringList(clmGet.source().get("paymentIds"));
                for (String pId : payIds) {
                    try {
                        client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_PAYMENTS).id(id + "_" + pId));
                    } catch (Exception ignored) {}
                }

                String pNo = helper.asString(clmGet.source().get("policyNo"));
                if (!pNo.isEmpty()) {
                    try {
                        GetResponse<Map> pGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(pNo), Map.class);
                        if (pGet.found() && pGet.source() != null) {
                            List<String> cList = helper.asStringList(pGet.source().get("claimIds"));
                            if (cList.remove(id)) {
                                client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(pNo).doc(Map.of("claimIds", cList)), Map.class);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Delete dependent payments by query as a sweep
            try {
                client.deleteByQuery(d -> d
                    .index(ElasticsearchIndexSetup.INDEX_PAYMENTS)
                    .refresh(true)
                    .query(q -> q.term(t -> t.field("claimId").value(id)))
                );
            } catch (Exception ignored) {}

            // Remove claimId from parent policy claimIds list
            SearchResponse<Map> polSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .query(q -> q.term(t -> t.field("claimIds").value(id)))
                .size(100),
                Map.class
            );
            for (Hit<Map> pHit : polSearch.hits().hits()) {
                Map<String, Object> pDoc = pHit.source();
                if (pDoc != null) {
                    String pNo = helper.asString(pDoc.get("policyNo"));
                    List<String> cIds = helper.asStringList(pDoc.get("claimIds"));
                    cIds.remove(id);
                    client.update(u -> u
                        .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                        .id(pNo)
                        .doc(Map.of("claimIds", cIds)),
                        Map.class
                    );
                }
            }

            // Delete claim document
            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_CLAIMS).id(id));
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch deleteClaim error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public int getClaimCount() {
        try {
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c.index(ElasticsearchIndexSetup.INDEX_CLAIMS));
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getClaimCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    public int getClaimsThisYearCount() {
        try {
            String startOfYear = LocalDate.now().getYear() + "-01-01";
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .query(q -> q.range(r -> r.date(d -> d.field("incidentDate").gte(startOfYear))))
            );
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getClaimsThisYearCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    public double getTotalClaimedAmount() {
        try {
            SearchResponse<Void> resp = ElasticsearchConnection.getClient().search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .size(0)
                .aggregations("totalClaimed", a -> a.sum(sm -> sm.field("claimedAmount"))),
                Void.class
            );

            Aggregate agg = resp.aggregations().get("totalClaimed");
            if (agg != null && agg.isSum()) {
                return agg.sum().value();
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch getTotalClaimedAmount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return 0.0;
    }

    @Override
    public List<String[]> getRecentClaims(int limit) {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> resp = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CLAIMS)
                .query(q -> q.matchAll(m -> m))
                .size(limit <= 0 ? 10 : limit)
                .sort(so -> so.field(f -> f.field("incidentDate").order(SortOrder.Desc))),
                Map.class
            );

            for (Hit<Map> hit : resp.hits().hits()) {
                Map<String, Object> doc = hit.source();
                if (doc != null) {
                    results.add(new String[] {
                        helper.asString(doc.get("claimId")),
                        helper.asString(doc.get("policyNo")),
                        helper.asString(doc.get("incidentDate")),
                        helper.asString(doc.get("description")),
                        String.valueOf(helper.asDouble(doc.get("claimedAmount"), 0.0)),
                        helper.asString(doc.get("status"))
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch getRecentClaims error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return results;
    }
}
