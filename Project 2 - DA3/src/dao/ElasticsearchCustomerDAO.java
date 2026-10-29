package dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import db.ElasticsearchConnection;
import service.ElasticsearchIndexSetup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch-backed DAO for Customer entities.
 * Directly queries and mutates the 'insurance_customers' index.
 * Preserves the exact public API of CustomerDAO for seamless Swing UI integration.
 */
public class ElasticsearchCustomerDAO extends CustomerDAO {

    private final AbstractElasticsearchDAO helper = new AbstractElasticsearchDAO() {};

    @Override
    public List<String[]> displayCustomers() {
        List<String[]> results = new ArrayList<>();
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            SearchResponse<Map> resp = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                .query(q -> q.matchAll(m -> m))
                .size(1000)
                .sort(so -> so.field(f -> f.field("personId").order(SortOrder.Asc))),
                Map.class
            );

            for (Hit<Map> hit : resp.hits().hits()) {
                Map<String, Object> doc = hit.source();
                if (doc != null) {
                    results.add(new String[] {
                        helper.asString(doc.get("personId")),
                        helper.asString(doc.get("firstName")),
                        helper.asString(doc.get("dob")),
                        helper.asString(doc.get("email")),
                        helper.asString(doc.get("aadhar")),
                        helper.asString(doc.get("address"))
                    });
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("Elasticsearch displayCustomers error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return null;
        }
    }

    @Override
    public String[] findCustomer(String personId) {
        if (personId == null || personId.trim().isEmpty()) return null;
        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            GetResponse<Map> resp = client.get(g -> g
                .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                .id(personId.trim()),
                Map.class
            );

            if (resp.found() && resp.source() != null) {
                Map<String, Object> doc = resp.source();
                return new String[] {
                    helper.asString(doc.get("personId")),
                    helper.asString(doc.get("firstName")),
                    helper.asString(doc.get("dob")),
                    helper.asString(doc.get("email")),
                    helper.asString(doc.get("aadhar")),
                    helper.asString(doc.get("address"))
                };
            }
        } catch (Exception e) {
            System.err.println("Elasticsearch findCustomer error: " + ElasticsearchConnection.sanitize(e.getMessage()));
        }
        return null;
    }

    @Override
    public boolean addCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {
        return addCustomer(personId, firstName, dob, email, aadhar, address, null);
    }

    public boolean addCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address,
            List<String> policyIds) {

        if (personId == null || personId.trim().isEmpty()) return false;
        String id = personId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();

            // Verify unique ID
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(id)).value();
            if (exists) return false;

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("personId", id);
            doc.put("firstName", helper.asString(firstName));
            if (dob != null && !dob.trim().isEmpty()) doc.put("dob", dob.trim());
            doc.put("email", helper.asString(email));
            doc.put("aadhar", helper.asString(aadhar));
            doc.put("address", helper.asString(address));
            doc.put("policyIds", (policyIds != null) ? new ArrayList<>(policyIds) : new ArrayList<String>());

            client.index(i -> i
                .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                .id(id)
                .document(doc)
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch addCustomer error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean updateCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        if (personId == null || personId.trim().isEmpty()) return false;
        String id = personId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(id)).value();
            if (!exists) return false;

            Map<String, Object> updates = new LinkedHashMap<>();
            updates.put("personId", id);
            updates.put("firstName", helper.asString(firstName));
            if (dob != null && !dob.trim().isEmpty()) updates.put("dob", dob.trim());
            updates.put("email", helper.asString(email));
            updates.put("aadhar", helper.asString(aadhar));
            updates.put("address", helper.asString(address));

            client.update(u -> u
                .index(ElasticsearchIndexSetup.INDEX_CUSTOMERS)
                .id(id)
                .doc(updates),
                Map.class
            );

            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch updateCustomer error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteCustomer(String personId) {
        if (personId == null || personId.trim().isEmpty()) return false;
        String id = personId.trim();

        try {
            ElasticsearchClient client = ElasticsearchConnection.getClient();
            boolean exists = client.exists(e -> e.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(id)).value();
            if (!exists) return false;

            // Collect all candidate policies linked to this customer (via query or customer's policyIds)
            java.util.Set<String> candidatePolicies = new java.util.LinkedHashSet<>();

            GetResponse<Map> custGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(id), Map.class);
            if (custGet.found() && custGet.source() != null) {
                candidatePolicies.addAll(helper.asStringList(custGet.source().get("policyIds")));
            }

            SearchResponse<Map> polSearch = client.search(s -> s
                .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                .query(q -> q.term(t -> t.field("customerIds").value(id)))
                .size(500),
                Map.class
            );
            for (Hit<Map> polHit : polSearch.hits().hits()) {
                candidatePolicies.add(polHit.id());
            }

            ElasticsearchPolicyDAO policyDAO = new ElasticsearchPolicyDAO();

            for (String policyNo : candidatePolicies) {
                GetResponse<Map> polGet = client.get(g -> g.index(ElasticsearchIndexSetup.INDEX_POLICIES).id(policyNo), Map.class);
                if (polGet.found() && polGet.source() != null) {
                    List<String> owners = helper.asStringList(polGet.source().get("customerIds"));
                    if (owners.isEmpty() || (owners.size() == 1 && owners.contains(id))) {
                        // Exclusively owned: cascade delete policy and its claims/payments/asset links
                        policyDAO.deletePolicy(policyNo);
                    } else {
                        // Shared policy: remove customer from owners list
                        owners.remove(id);
                        client.update(u -> u
                            .index(ElasticsearchIndexSetup.INDEX_POLICIES)
                            .id(policyNo)
                            .doc(Map.of("customerIds", owners)),
                            Map.class
                        );
                    }
                }
            }

            // Delete customer document
            client.delete(d -> d.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS).id(id));
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_POLICIES);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_CLAIMS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_PAYMENTS);
            helper.refreshIndex(ElasticsearchIndexSetup.INDEX_ASSETS);
            return true;
        } catch (Exception e) {
            System.err.println("Elasticsearch deleteCustomer error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return false;
        }
    }

    @Override
    public int getCustomerCount() {
        try {
            CountResponse resp = ElasticsearchConnection.getClient().count(c -> c.index(ElasticsearchIndexSetup.INDEX_CUSTOMERS));
            return (int) resp.count();
        } catch (Exception e) {
            System.err.println("Elasticsearch getCustomerCount error: " + ElasticsearchConnection.sanitize(e.getMessage()));
            return 0;
        }
    }
}
