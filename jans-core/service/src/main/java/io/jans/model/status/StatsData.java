/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.model.status;

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
