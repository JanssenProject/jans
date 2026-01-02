package io.jans.as.model.configuration.rate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimitRule {

    private String endpoint;
    private List<String> methods = new ArrayList<>();
    private Integer requestCount;
    private Integer periodInSeconds;
    private List<KeyExtractor> keyExtractors = new ArrayList<>();

    public RateLimitRule() {
    }

    @JsonCreator
    public RateLimitRule(
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("methods") List<String> methods,
            @JsonProperty("requestCount") Integer requestCount,
            @JsonProperty("periodInSeconds") Integer periodInSeconds,
            @JsonProperty("keyExtractors") List<KeyExtractor> keyExtractors
    ) {
        setEndpoint(endpoint);
        setMethods(methods);
        setRequestCount(requestCount);
        setPeriodInSeconds(periodInSeconds);
        setKeyExtractors(keyExtractors);
    }

    @JsonProperty("endpoint")
    public String getEndpoint() {
        return endpoint;
    }

    @JsonProperty("endpoint")
    public void setEndpoint(String endpoint) {
        this.endpoint = (endpoint == null || endpoint.trim().isEmpty()) ? null : endpoint.trim();
    }

    @JsonProperty("methods")
    public List<String> getMethods() {
        return methods == null ? Collections.emptyList() : Collections.unmodifiableList(methods);
    }

    @JsonProperty("methods")
    public void setMethods(List<String> methods) {
        // Defensive copy + filter null/blank
        List<String> safe = new ArrayList<>();
        if (methods != null) {
            for (String m : methods) {
                if (m == null) continue;
                String v = m.trim();
                if (!v.isEmpty()) safe.add(v);
            }
        }
        this.methods = safe;
    }

    @JsonProperty("requestCount")
    public Integer getRequestCount() {
        return requestCount;
    }

    @JsonProperty("requestCount")
    public void setRequestCount(Integer requestCount) {
        this.requestCount = (requestCount != null && requestCount > 0) ? requestCount : null;
    }

    @JsonProperty("periodInSeconds")
    public Integer getPeriodInSeconds() {
        return periodInSeconds;
    }

    @JsonProperty("periodInSeconds")
    public void setPeriodInSeconds(Integer periodInSeconds) {
        this.periodInSeconds = (periodInSeconds != null && periodInSeconds > 0) ? periodInSeconds : null;
    }

    @JsonProperty("keyExtractors")
    public List<KeyExtractor> getKeyExtractors() {
        return keyExtractors == null ? Collections.emptyList() : Collections.unmodifiableList(keyExtractors);
    }

    @JsonProperty("keyExtractors")
    public void setKeyExtractors(List<KeyExtractor> keyExtractors) {
        List<KeyExtractor> safe = new ArrayList<>();
        if (keyExtractors != null) {
            for (KeyExtractor ke : keyExtractors) {
                if (ke != null) safe.add(ke);
            }
        }
        this.keyExtractors = safe;
    }

    /**
     * Non-throwing helper for validation-style checks in business logic.
     */
    public boolean isWellFormed() {
        return endpoint != null
                && !getMethods().isEmpty()
                && requestCount != null
                && periodInSeconds != null
                && !getKeyExtractors().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitRule)) return false;
        RateLimitRule that = (RateLimitRule) o;
        return Objects.equals(endpoint, that.endpoint)
                && Objects.equals(getMethods(), that.getMethods())
                && Objects.equals(requestCount, that.requestCount)
                && Objects.equals(periodInSeconds, that.periodInSeconds)
                && Objects.equals(getKeyExtractors(), that.getKeyExtractors());
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, getMethods(), requestCount, periodInSeconds, getKeyExtractors());
    }

    @Override
    public String toString() {
        return "RateLimitRule{" +
                "endpoint='" + endpoint + '\'' +
                ", methods=" + getMethods() +
                ", requestCount=" + requestCount +
                ", periodInSeconds=" + periodInSeconds +
                ", keyExtractors=" + getKeyExtractors() +
                '}';
    }
}
