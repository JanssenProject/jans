/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.bulk;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A class representing the components of a SCIM Bulk operation as per section 3.7 of RFC 7644.
 * @author Rahat Ali Date: 05.08.2015
 */
/*
 * Updated by jgomer on 2017-11-21.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkOperation {

    private static ObjectMapper mapper=new ObjectMapper();

    private String method;
    private String bulkId;
    //private String version;
    private String path;
    private Map<String, Object> data;
    private String location;
    private Object response;
    private String status;

    @JsonIgnore
    private String dataStr;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getBulkId() {
        return bulkId;
    }

    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }
/*
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
*/

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDataStr() {
        return dataStr;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> map) {

        try {
            data=map;
            dataStr = mapper.writeValueAsString(map);
        }
        catch (Exception e){
            dataStr=null;
        }

    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
