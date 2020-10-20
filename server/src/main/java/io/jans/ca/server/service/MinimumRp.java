package io.jans.ca.server.service;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
public class MinimumRp implements Serializable {

    @JsonProperty(value = "rp_id")
    private String rpId;

    @JsonProperty(value = "client_name")
    private String clientName;

    public MinimumRp() {
    }

    public MinimumRp(String rpId, String clientName) {
        this.rpId = rpId;
        this.clientName = clientName;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
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
                "rpId='" + rpId + '\'' +
                ", clientName='" + clientName + '\'' +
                '}';
    }
}
