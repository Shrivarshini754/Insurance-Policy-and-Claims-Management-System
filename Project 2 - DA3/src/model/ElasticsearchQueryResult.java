package model;

/**
 * Data carrier for Elasticsearch Query Console execution results.
 * Holds response status, timing, pretty-printed JSON payload, hit counts,
 * and sanitized error messages.
 */
public class ElasticsearchQueryResult {

    private final boolean success;
    private final int statusCode;
    private final long executionTimeMs;
    private final String endpoint;
    private final String responseBody;
    private final String errorMessage;
    private final long totalHits;
    private final boolean hasTotalHits;
    private final int returnedHitsCount;

    public ElasticsearchQueryResult(
            boolean success,
            int statusCode,
            long executionTimeMs,
            String endpoint,
            String responseBody,
            String errorMessage,
            long totalHits,
            boolean hasTotalHits,
            int returnedHitsCount) {
        this.success = success;
        this.statusCode = statusCode;
        this.executionTimeMs = executionTimeMs;
        this.endpoint = endpoint;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
        this.totalHits = totalHits;
        this.hasTotalHits = hasTotalHits;
        this.returnedHitsCount = returnedHitsCount;
    }

    public static ElasticsearchQueryResult failure(int statusCode, String endpoint, String errorMessage, String rawResponse) {
        return new ElasticsearchQueryResult(false, statusCode, 0, endpoint, rawResponse, errorMessage, 0, false, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public boolean hasTotalHits() {
        return hasTotalHits;
    }

    public int getReturnedHitsCount() {
        return returnedHitsCount;
    }
}
