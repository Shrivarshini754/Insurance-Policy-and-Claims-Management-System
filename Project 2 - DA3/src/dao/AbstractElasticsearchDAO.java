package dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import db.ElasticsearchConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base utility class for Elasticsearch-backed Data Access Objects.
 * Provides client access, type-safe field extraction, and automatic refresh helpers.
 */
public abstract class AbstractElasticsearchDAO {

    protected ElasticsearchClient getClient() {
        return ElasticsearchConnection.getClient();
    }

    protected String asString(Object val) {
        if (val == null) return "";
        return val.toString().trim();
    }

    protected double asDouble(Object val) {
        return asDouble(val, 0.0);
    }

    protected double asDouble(Object val, double defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    protected int asInt(Object val) {
        return asInt(val, 0);
    }

    protected int asInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> asStringList(Object val) {
        List<String> list = new ArrayList<>();
        if (val == null) return list;
        if (val instanceof Collection<?>) {
            for (Object item : (Collection<?>) val) {
                if (item != null) {
                    String str = item.toString().trim();
                    if (!str.isEmpty() && !list.contains(str)) {
                        list.add(str);
                    }
                }
            }
        } else {
            String str = val.toString().trim();
            if (!str.isEmpty()) list.add(str);
        }
        return list;
    }

    /**
     * Refreshes the target index so write operations (insert, update, delete)
     * are immediately visible to subsequent search reads in the Swing UI.
     */
    protected void refreshIndex(String indexName) {
        try {
            getClient().indices().refresh(r -> r.index(indexName));
        } catch (Exception e) {
            // Non-critical; near-real-time refresh will still occur automatically
        }
    }
}
