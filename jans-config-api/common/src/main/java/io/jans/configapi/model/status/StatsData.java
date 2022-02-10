package io.jans.configapi.model.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "auth_server_status", "db_type", "db_status", "facter_data", "last-update" })
public class StatsData {

    @JsonProperty("auth_server_status")
    private String authServerStatus;

    @JsonProperty("db_type")
    private String dbType;

    @JsonProperty("db_status")
    private String dbStatus;

    @JsonProperty("facter_data")
    private FacterData facterData;
    
    @JsonProperty("last-update")
    private Date lastUpdate;

    public String getAuthServerStatus() {
        return authServerStatus;
    }

    public void setAuthServerStatus(String authServerStatus) {
        this.authServerStatus = authServerStatus;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public void setDbStatus(String dbStatus) {
        this.dbStatus = dbStatus;
    }

    public FacterData getFacterData() {
        return facterData;
    }

    public void setFacterData(FacterData facterData) {
        this.facterData = facterData;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return "StatsData [authServerStatus=" + authServerStatus + ", dbType=" + dbType + ", dbStatus=" + dbStatus
                + ", facterData=" + facterData + ", lastUpdate=" + lastUpdate + "]";
    }

}
