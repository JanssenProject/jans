package io.jans.as.server.ws.rs.stat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class StatResponseItem {

    @JsonProperty
    private String month;

    @JsonProperty(value = "monthly_active_users")
    private long monthlyActiveUsers;

    @JsonProperty("token_count_per_granttype")
    private Map<String, Map<String, Long>> tokenCountPerGrantType;

    public long getMonthlyActiveUsers() {
        return monthlyActiveUsers;
    }

    public void setMonthlyActiveUsers(long monthlyActiveUsers) {
        this.monthlyActiveUsers = monthlyActiveUsers;
    }

    public Map<String, Map<String, Long>> getTokenCountPerGrantType() {
        if (tokenCountPerGrantType == null) tokenCountPerGrantType = new HashMap<>();
        return tokenCountPerGrantType;
    }

    public void setTokenCountPerGrantType(Map<String, Map<String, Long>> tokenCountPerGrantType) {
        this.tokenCountPerGrantType = tokenCountPerGrantType;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @Override
    public String toString() {
        return "StatResponseItem{" +
                "monthlyActiveUsers=" + monthlyActiveUsers +
                ", month=" + month +
                ", tokenCountPerGrantType=" + tokenCountPerGrantType +
                '}';
    }
}

