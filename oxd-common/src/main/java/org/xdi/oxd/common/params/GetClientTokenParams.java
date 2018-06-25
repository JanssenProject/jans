package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetClientTokenParams implements IParams {

    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "op_host")
    private String opHost;
    @JsonProperty(value = "op_discovery_path")
    private String opDiscoveryPath;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "authentication_method")
    private String authenticationMethod;
    @JsonProperty(value = "algorithm")
    private String algorithm;
    @JsonProperty(value = "key_id")
    private String keyId;

    public String getOpDiscoveryPath() {
        return opDiscoveryPath;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.opDiscoveryPath = opDiscoveryPath;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    @Override
    public String toString() {
        return "GetClientTokenParams{" +
                "clientId='" + clientId + '\'' +
                ", opHost='" + opHost + '\'' +
                ", opDiscoveryPath='" + opDiscoveryPath + '\'' +
                ", scope=" + scope +
                ", authenticationMethod='" + authenticationMethod + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", keyId='" + keyId + '\'' +
                '}';
    }
}
