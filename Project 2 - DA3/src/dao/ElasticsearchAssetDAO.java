package dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import db.ElasticsearchConnection;
import service.ElasticsearchIndexSetup;

import java.util.*;

/**
 * ElasticsearchAssetDAO - Elasticsearch implementation of AssetDAO.
 * Preserves all public method signatures, field mappings, cascade rules,
 * and immediate index refreshing.
 */
public class ElasticsearchAssetDAO extends AssetDAO {

    private final AbstractElasticsearchDAO helper = new AbstractElasticsearchDAO() {};

    @Override
    @SuppressWarnings("unchecked")
    public List<String[]> displayAssets() {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> response = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                .size(10000)
                .query(q -> q.matchAll(m -> m)),
                Map.class
            );

            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> src = hit.source();
                if (src == null) continue;

                String assetId = hit.id();
                if (src.containsKey("assetId")) {
                    assetId = helper.asString(src.get("assetId"));
                }
                String description = helper.asString(src.get("description"));
                String purchaseDate = helper.asString(src.get("purchaseDate"));
                double estimatedValue = helper.asDouble(src.get("estimatedValue"));

                results.add(new String[] {
                    assetId,
                    description,
                    purchaseDate,
                    String.valueOf(estimatedValue)
                });
            }

            results.sort(Comparator.comparing((String[] row) -> row[0]));
        } catch (Exception e) {
            System.err.println("Elasticsearch displayAssets error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return results;
    }

    @Override
    public boolean addAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {
        return addAsset(assetId, description, purchaseDate, estimatedValue, null);
    }

    public boolean addAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue,
            List<String> policyIds) {

        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        String id = assetId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(id)).value();
            if (exists) {
                return false;
            }

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("assetId", id);
            doc.put("description", helper.asString(description));
            doc.put("purchaseDate", purchaseDate != null ? purchaseDate.trim() : "");
            doc.put("estimatedValue", estimatedValue);
            doc.put("policyIds", (policyIds != null) ? new ArrayList<>(policyIds) : new ArrayList<String>());

            client.index(i -> i
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                .id(id)
                .document(doc)
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch addAsset error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean updateAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        String id = assetId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(id)).value();
            if (!exists) {
                return false;
            }

            Map<String, Object> updates = new LinkedHashMap<>();
            updates.put("assetId", id);
            updates.put("description", helper.asString(description));
            updates.put("purchaseDate", purchaseDate != null ? purchaseDate.trim() : "");
            updates.put("estimatedValue", estimatedValue);

            client.update(u -> u
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                .id(id)
                .doc(updates),
                Map.class
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch updateAsset error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }

        String id = assetId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(id)).value();
            if (!exists) {
                System.err.println("Asset '" + id + "' does not exist.");
                return false;
            }

            ElasticsearchPolicyDAO policyDAO = new ElasticsearchPolicyDAO();

            // 1. Inspect policies listed in this asset's policyIds array
            GetResponse<Map> astGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(id), Map.class);
            if (astGet.found() && astGet.source() != null) {
                List<String> pols = helper.asStringList(astGet.source().get("policyIds"));
                for (String pNo : pols) {
                    GetResponse<Map> pGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(pNo), Map.class);
                    if (pGet.found() && pGet.source() != null) {
                        List<String> assetIds = helper.asStringList(pGet.source().get("assetIds"));
                        List<String> custIds = helper.asStringList(pGet.source().get("customerIds"));
                        if ((assetIds.isEmpty() || (assetIds.size() == 1 && assetIds.contains(id))) && custIds.isEmpty()) {
                            policyDAO.deletePolicy(pNo);
                        } else {
                            assetIds.remove(id);
                            client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(pNo).doc(Map.of("assetIds", assetIds)), Map.class);
                        }
                    }
                }
            }

            // 2. Inspect policies referencing this asset via policy.assetIds query
            SearchResponse<Map> polSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .size(1000)
                .query(q -> q.matchAll(m -> m)),
                Map.class
            );

            for (Hit<Map> hit : polSearch.hits().hits()) {
                Map<String, Object> pSrc = hit.source();
                if (pSrc == null) continue;

                List<String> assetIds = helper.asStringList(pSrc.get("assetIds"));
                if (assetIds.contains(id)) {
                    List<String> custIds = helper.asStringList(pSrc.get("customerIds"));
                    // If orphan policy exclusively covering this asset with no customer owner, cascade delete
                    if (assetIds.size() == 1 && custIds.isEmpty()) {
                        policyDAO.deletePolicy(hit.id());
                    } else {
                        // Otherwise detach asset from policy
                        assetIds.remove(id);
                        Map<String, Object> pUp = new LinkedHashMap<>();
                        pUp.put("assetIds", assetIds);
                        client.update(u -> u.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(hit.id()).doc(pUp), Map.class);
                    }
                }
            }

            // Delete asset document
            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_ASSETS).id(id));

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch deleteAsset error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public int getAssetCount() {
        try {
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
            );
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getAssetCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String[] findAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return null;
        }

        String id = assetId.trim();

        try {
            GetResponse<Map> resp = ElasticsearchConnection.getClient().get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_ASSETS)
                .id(id),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                Map<String, Object> src = resp.source();
                return new String[] {
                    src.containsKey("assetId") ? helper.asString(src.get("assetId")) : id,
                    helper.asString(src.get("description")),
                    helper.asString(src.get("purchaseDate")),
                    String.valueOf(helper.asDouble(src.get("estimatedValue")))
                };
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch findAsset error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return null;
    }
}
