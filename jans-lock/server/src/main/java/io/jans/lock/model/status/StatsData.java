/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.status;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
