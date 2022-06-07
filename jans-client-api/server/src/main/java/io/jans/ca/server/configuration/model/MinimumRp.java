package io.jans.ca.server.configuration.model;


import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
public class MinimumRp implements Serializable {

    private String rpId;
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
