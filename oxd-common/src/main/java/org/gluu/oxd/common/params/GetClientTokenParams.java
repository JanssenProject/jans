package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetClientTokenParams implements IParams {

    @JsonProperty(value = "client_id")
    private String client_id;
    @JsonProperty(value = "client_secret")
    private String client_secret;
    @JsonProperty(value = "op_host")
    private String op_host;
    @JsonProperty(value = "op_discovery_path")
    private String op_discovery_path;
    @JsonProperty(value = "op_configuration_endpoint")
    private String op_configuration_endpoint;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "authentication_method")
    private String authentication_method;
    @JsonProperty(value = "algorithm")
    private String algorithm;
    @JsonProperty(value = "key_id")
    private String key_id;

    public String getOpDiscoveryPath() {
        return op_discovery_path;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.op_discovery_path = opDiscoveryPath;
    }

    public String getOpConfigurationEndpoint() {
        return op_configuration_endpoint;
    }

    public void setOpConfigurationEndpoint(String op_configuration_endpoint) {
        this.op_configuration_endpoint = op_configuration_endpoint;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String clientId) {
        this.client_id = clientId;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(String clientSecret) {
        this.client_secret = clientSecret;
    }

    public String getOpHost() {
        return op_host;
    }

    public void setOpHost(String opHost) {
        this.op_host = opHost;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getAuthenticationMethod() {
        return authentication_method;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authentication_method = authenticationMethod;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyId() {
        return key_id;
    }

    public void setKeyId(String keyId) {
        this.key_id = keyId;
    }

    @Override
    public String toString() {
        return "GetClientTokenParams{" +
                "client_id='" + client_id + '\'' +
                ", op_host='" + op_host + '\'' +
                ", op_discovery_path='" + op_discovery_path + '\'' +
                ", op_configuration_endpoint='" + op_configuration_endpoint + '\'' +
                ", scope=" + scope +
                ", authentication_method='" + authentication_method + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", key_id='" + key_id + '\'' +
                '}';
    }
}
