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
public class RateLimitConfig {

    private List<RateLimitRule> rateLimitRules = new ArrayList<>();

    public RateLimitConfig() {
    }

    @JsonCreator
    public RateLimitConfig(@JsonProperty("rateLimitRules") List<RateLimitRule> rateLimitRules) {
        setRateLimitRules(rateLimitRules);
    }

    @JsonProperty("rateLimitRules")
    public List<RateLimitRule> getRateLimitRules() {
        return rateLimitRules == null ? Collections.emptyList() : Collections.unmodifiableList(rateLimitRules);
    }

    @JsonProperty("rateLimitRules")
    public void setRateLimitRules(List<RateLimitRule> rateLimitRules) {
        this.rateLimitRules = (rateLimitRules == null) ? new ArrayList<>() : new ArrayList<>(rateLimitRules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitConfig)) return false;
        RateLimitConfig that = (RateLimitConfig) o;
        return Objects.equals(getRateLimitRules(), that.getRateLimitRules());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRateLimitRules());
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "rateLimitRules=" + getRateLimitRules() +
                '}';
    }
}
