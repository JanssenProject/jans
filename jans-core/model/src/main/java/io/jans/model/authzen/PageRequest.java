package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * AuthZEN Pagination Request.
 * Used for search endpoints pagination.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageRequest implements Serializable {

    @JsonProperty("token")
    private String token;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("properties")
    private JsonNode properties;

    public PageRequest() {
    }

    public String getToken() {
        return token;
    }

    public PageRequest setToken(String token) {
        this.token = token;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public PageRequest setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public PageRequest setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "token='" + token + '\'' +
                ", limit=" + limit +
                ", properties=" + properties +
                '}';
    }
}
