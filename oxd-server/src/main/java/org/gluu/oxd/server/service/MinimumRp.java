package org.gluu.oxd.server.service;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
public class MinimumRp implements Serializable {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    @JsonProperty(value = "client_name")
    private String clientName;

    public MinimumRp() {
    }

    public MinimumRp(String oxdId, String clientName) {
        this.oxdId = oxdId;
        this.clientName = clientName;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String toString() {
        return "MinimumRp{" +
                "oxdId='" + oxdId + '\'' +
                ", clientName='" + clientName + '\'' +
                '}';
    }
}
