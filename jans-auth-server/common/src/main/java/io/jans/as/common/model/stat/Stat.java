package io.jans.as.common.model.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stat implements Serializable {

    @JsonProperty("tokenCountPerGrantType")
    private Map<String, Map<String, Long>> tokenCountPerGrantType;
    @JsonProperty("lastUpdatedAt")
    private long lastUpdatedAt;
    @JsonProperty("month")
    private String month;

    public Map<String, Map<String, Long>> getTokenCountPerGrantType() {
        if (tokenCountPerGrantType == null) tokenCountPerGrantType = new HashMap<>();
        return tokenCountPerGrantType;
    }

    public void setTokenCountPerGrantType(Map<String, Map<String, Long>> tokenCountPerGrantType) {
        this.tokenCountPerGrantType = tokenCountPerGrantType;
    }

    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @Override
    public String toString() {
        return "Stat{" +
                "tokenCountPerGrantType=" + tokenCountPerGrantType +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", month='" + month + '\'' +
                '}';
    }
}

