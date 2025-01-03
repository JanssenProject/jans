package io.jans.lock.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stat implements Serializable {

	private static final long serialVersionUID = -1659698750177377994L;

	@JsonProperty("countOpByType")
    private Map<String, Map<String, Long>> operationsByType;

    @JsonProperty("lastUpdatedAt")
    private long lastUpdatedAt;

    @JsonProperty("month")
    private String month;

    public Map<String, Map<String, Long>> getOperationsByType() {
        if (operationsByType == null) operationsByType = new HashMap<>();
        return operationsByType;
    }

    public void setOperationsByType(Map<String, Map<String, Long>> operationsByType) {
        this.operationsByType = operationsByType;
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
                "operationsByType=" + operationsByType +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", month='" + month + '\'' +
                '}';
    }
}

