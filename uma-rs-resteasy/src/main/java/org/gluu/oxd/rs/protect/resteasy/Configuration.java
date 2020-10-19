package org.gluu.oxd.rs.protect.resteasy;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 */

public class Configuration {

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    @JsonProperty(value = "op_host")
    private String opHost;
    @JsonProperty(value = "pat_client_id")
    private String umaPatClientId;
    @JsonProperty(value = "pat_client_secret")
    private String umaPatClientSecret;
    @JsonProperty(value = "trust_all")
    private boolean trustAll;

    public Configuration() {
    }

    public boolean isTrustAll() {
        return trustAll;
    }

    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    public String wellKnownEndpoint() {
        return opHost + WELL_KNOWN_UMA_PATH;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getUmaPatClientId() {
        return umaPatClientId;
    }

    public void setUmaPatClientId(String umaPatClientId) {
        this.umaPatClientId = umaPatClientId;
    }

    public String getUmaPatClientSecret() {
        return umaPatClientSecret;
    }

    public void setUmaPatClientSecret(String umaPatClientSecret) {
        this.umaPatClientSecret = umaPatClientSecret;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration");
        sb.append("{opHost='").append(opHost).append('\'');
        sb.append(", umaPatClientId='").append(umaPatClientId).append('\'');
        sb.append(", umaPatClientSecret='").append(umaPatClientSecret).append('\'');
        sb.append(", trustAll='").append(trustAll).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
