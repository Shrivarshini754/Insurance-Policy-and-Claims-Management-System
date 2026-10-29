package dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.ElasticsearchConnection;
import model.ElasticsearchQueryResult;
import model.QueryResult;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ElasticsearchQueryDAO - Data Access Object for Elasticsearch Query Console.
 * Validates JSON Query DSL, restricts requests to safe read-only operations (_search, _count),
 * executes queries against Elastic Cloud via RestClient, and formats server responses.
 */
public class ElasticsearchQueryDAO extends QueryDAO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> ALLOWED_ENDPOINTS = Set.of("_search", "_count");

    @Override
    public QueryResult executeQuery(String query) {
        QueryResult result = new QueryResult();
        result.setSuccess(false);
        result.setErrorMessage(
            "Cypher query execution is unavailable in Elasticsearch backend mode. " +
            "Use the Elasticsearch Query Console or switch to Neo4j backend (-Ddb.backend=neo4j)."
        );
        List<String> cols = new ArrayList<>();
        cols.add("Notice");
        result.setColumnNames(cols);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "Cypher console disabled in Elasticsearch mode." });
        result.setRows(rows);
        return result;
    }

    /**
     * Executes a read-only Query DSL request against Elasticsearch.
     * Enforces JSON validation, strictly restricts endpoints to _search and _count,
     * formats the JSON response, and extracts hit metrics.
     *
     * @param index    target Elasticsearch index name
     * @param endpoint target read-only endpoint (_search or _count)
     * @param jsonBody raw JSON request body
     * @return ElasticsearchQueryResult containing formatted response and metrics
     */
    public ElasticsearchQueryResult executeElasticsearchQuery(String index, String endpoint, String jsonBody) {
        if (index == null || index.trim().isEmpty()) {
            return ElasticsearchQueryResult.failure(400, endpoint, "Target index cannot be null or empty.", null);
        }
        String cleanIndex = index.trim();
        if (cleanIndex.contains("/") || cleanIndex.contains("..") || cleanIndex.contains("?") || cleanIndex.contains("#")) {
            return ElasticsearchQueryResult.failure(400, endpoint, "Invalid index name contains prohibited path characters.", null);
        }

        if (endpoint == null || endpoint.trim().isEmpty()) {
            return ElasticsearchQueryResult.failure(400, endpoint, "Target endpoint cannot be null or empty.", null);
        }
        String cleanEndpoint = endpoint.trim();
        if (!cleanEndpoint.startsWith("_")) {
            cleanEndpoint = "_" + cleanEndpoint;
        }

        // Enforce strict read-only security rule
        if (!ALLOWED_ENDPOINTS.contains(cleanEndpoint)) {
            return ElasticsearchQueryResult.failure(
                403,
                cleanEndpoint,
                "Security policy violation: Endpoint '" + cleanEndpoint + "' is rejected. " +
                "Only read-only endpoints ('_search' and '_count') are permitted in the query console.",
                null
            );
        }

        // Validate JSON request body syntax
        String requestPayload = (jsonBody != null) ? jsonBody.trim() : "";
        if (requestPayload.isEmpty()) {
            requestPayload = "{}";
        } else {
            try {
                // Verify syntax validity
                JsonNode parsed = OBJECT_MAPPER.readTree(requestPayload);
                if (!parsed.isObject()) {
                    return ElasticsearchQueryResult.failure(
                        400,
                        cleanEndpoint,
                        "JSON validation error: Request body must be a valid JSON object starting with '{'.",
                        null
                    );
                }
            } catch (JsonProcessingException e) {
                return ElasticsearchQueryResult.failure(
                    400,
                    cleanEndpoint,
                    "JSON syntax error: " + e.getOriginalMessage(),
                    null
                );
            }
        }

        String fullEndpoint = "POST /" + cleanIndex + "/" + cleanEndpoint;

        try {
            Request request = new Request("POST", "/" + cleanIndex + "/" + cleanEndpoint);
            request.setJsonEntity(requestPayload);

            long startTime = System.currentTimeMillis();
            Response response = ElasticsearchConnection.getRestClient().performRequest(request);
            long elapsed = System.currentTimeMillis() - startTime;

            int statusCode = response.getStatusLine().getStatusCode();
            String responseText = "";
            if (response.getEntity() != null) {
                try (InputStream is = response.getEntity().getContent()) {
                    responseText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            // Pretty-print response JSON
            String prettyResponse = responseText;
            long totalHits = 0;
            boolean hasTotalHits = false;
            int returnedHits = 0;

            try {
                JsonNode root = OBJECT_MAPPER.readTree(responseText);
                prettyResponse = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);

                if ("_search".equals(cleanEndpoint)) {
                    JsonNode hitsNode = root.path("hits");
                    if (!hitsNode.isMissingNode()) {
                        JsonNode totalNode = hitsNode.path("total");
                        if (totalNode.isNumber()) {
                            totalHits = totalNode.asLong();
                            hasTotalHits = true;
                        } else if (totalNode.isObject() && totalNode.has("value")) {
                            totalHits = totalNode.path("value").asLong();
                            hasTotalHits = true;
                        }

                        JsonNode hitsArray = hitsNode.path("hits");
                        if (hitsArray.isArray()) {
                            returnedHits = hitsArray.size();
                        }
                    }
                } else if ("_count".equals(cleanEndpoint)) {
                    JsonNode countNode = root.path("count");
                    if (countNode.isNumber()) {
                        totalHits = countNode.asLong();
                        hasTotalHits = true;
                        returnedHits = (int) totalHits;
                    }
                }
            } catch (Exception ignored) {
                // Keep raw response if formatting fails
            }

            return new ElasticsearchQueryResult(
                true,
                statusCode,
                elapsed,
                fullEndpoint,
                prettyResponse,
                null,
                totalHits,
                hasTotalHits,
                returnedHits
            );

        } catch (ResponseException e) {
            int statusCode = e.getResponse().getStatusLine().getStatusCode();
            String rawErrorBody = "";
            try {
                if (e.getResponse().getEntity() != null) {
                    try (InputStream is = e.getResponse().getEntity().getContent()) {
                        rawErrorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception ignored) { }

            // Pretty print error body if JSON
            String prettyError = ElasticsearchConnection.sanitize(rawErrorBody);
            try {
                JsonNode errNode = OBJECT_MAPPER.readTree(rawErrorBody);
                prettyError = ElasticsearchConnection.sanitize(
                    OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(errNode)
                );
            } catch (Exception ignored) { }

            String sanitizedMsg = ElasticsearchConnection.sanitize(e.getMessage());
            return new ElasticsearchQueryResult(
                false,
                statusCode,
                0,
                fullEndpoint,
                prettyError,
                "Elasticsearch HTTP " + statusCode + " Error: " + sanitizedMsg,
                0,
                false,
                0
            );

        } catch (Exception e) {
            String sanitizedMsg = ElasticsearchConnection.sanitize(
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
            return ElasticsearchQueryResult.failure(500, fullEndpoint, sanitizedMsg, null);
        }
    }
}
