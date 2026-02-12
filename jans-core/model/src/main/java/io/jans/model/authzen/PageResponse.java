package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * AuthZEN Pagination Response.
 * Used for search endpoints pagination.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse {

    @JsonProperty("next_token")
    private String nextToken;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("properties")
    private JsonNode properties;

    public PageResponse() {
        // empty
    }

    public PageResponse(String nextToken, Integer count, Integer total) {
        this.nextToken = nextToken;
        this.count = count;
        this.total = total;
    }

    public String getNextToken() {
        return nextToken;
    }

    public PageResponse setNextToken(String nextToken) {
        this.nextToken = nextToken;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    public PageResponse setCount(Integer count) {
        this.count = count;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public PageResponse setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public PageResponse setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        return "PageResponse{" +
                "nextToken='" + nextToken + '\'' +
                ", count=" + count +
                ", total=" + total +
                ", properties=" + properties +
                '}';
    }
}
