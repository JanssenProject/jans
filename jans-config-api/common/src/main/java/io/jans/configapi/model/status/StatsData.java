package io.jans.configapi.model.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "dbType", "lastUpdate", "facterData", })
public class StatsData {

    @JsonProperty("dbType")
    private String dbType;

    @JsonProperty("lastUpdate")
    private Date lastUpdate;

    @JsonProperty("facterData")
    private FacterData facterData;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public FacterData getFacterData() {
        return facterData;
    }

    public void setFacterData(FacterData facterData) {
        this.facterData = facterData;
    }

    @Override
    public String toString() {
        return "StatsData [dbType=" + dbType + ", lastUpdate=" + lastUpdate + ", facterData=" + facterData + "]";
    }

}
