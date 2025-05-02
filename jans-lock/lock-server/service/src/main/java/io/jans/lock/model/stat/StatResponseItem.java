package io.jans.lock.model.stat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
public class StatResponseItem {

    @JsonProperty
    private String month;

    @JsonProperty(value = "monthly_active_users")
    private long monthlyActiveUsers;

    @JsonProperty(value = "monthly_active_clients")
    private long monthlyActiveClients;

    @JsonProperty("operations_by_type")
    private Map<String, Map<String, Long>> operationsByType;

    public long getMonthlyActiveUsers() {
        return monthlyActiveUsers;
    }

    public void setMonthlyActiveUsers(long monthlyActiveUsers) {
        this.monthlyActiveUsers = monthlyActiveUsers;
    }

    public long getMonthlyActiveClients() {
		return monthlyActiveClients;
	}

	public void setMonthlyActiveClients(long monthlyActiveClients) {
		this.monthlyActiveClients = monthlyActiveClients;
	}

	public Map<String, Map<String, Long>> getOperationsByType() {
        if (operationsByType == null) operationsByType = new HashMap<>();
        return operationsByType;
    }

    public void setOperationsByType(Map<String, Map<String, Long>> operationsByType) {
        this.operationsByType = operationsByType;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @Override
	public String toString() {
		return "StatResponseItem [month=" + month + ", monthlyActiveUsers=" + monthlyActiveUsers
				+ ", monthlyActiveClients=" + monthlyActiveClients + ", operationsByType=" + operationsByType + "]";
	}
}

