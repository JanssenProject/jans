package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RegisterPermissionTicketParams implements IParams {

    @JsonProperty(value = "uma_discovery_url")
    private String umaDiscoveryUrl;
    @JsonProperty(value = "pat")
    private String patToken;
    @JsonProperty(value = "am_host")
    private String amHost;
    @JsonProperty(value = "rs_host")
    private String rsHost;
    @JsonProperty(value = "resource_set_id")
    private String resourceSetId;
    @JsonProperty(value = "scopes")
    private List<String> scopes;
    @JsonProperty(value = "request_http_method")
    private String requestHttpMethod;
    @JsonProperty(value = "request_url")
    private String requestUrl;

    public RegisterPermissionTicketParams() {
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> p_scopes) {
        scopes = p_scopes;
    }

    public String getUmaDiscoveryUrl() {
        return umaDiscoveryUrl;
    }

    public void setUmaDiscoveryUrl(String p_umaDiscoveryUrl) {
        umaDiscoveryUrl = p_umaDiscoveryUrl;
    }

    public String getPatToken() {
        return patToken;
    }

    public void setPatToken(String p_patToken) {
        patToken = p_patToken;
    }

    public String getAmHost() {
        return amHost;
    }

    public void setAmHost(String p_amHost) {
        amHost = p_amHost;
    }

    public String getRsHost() {
        return rsHost;
    }

    public void setRsHost(String p_rsHost) {
        rsHost = p_rsHost;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String p_resourceSetId) {
        resourceSetId = p_resourceSetId;
    }

    public String getRequestHttpMethod() {
        return requestHttpMethod;
    }

    public void setRequestHttpMethod(String requestHttpMethod) {
        this.requestHttpMethod = requestHttpMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterPermissionTicketParams");
        sb.append("{umaDiscoveryUrl='").append(umaDiscoveryUrl).append('\'');
        sb.append(", patToken='").append(patToken).append('\'');
        sb.append(", amHost='").append(amHost).append('\'');
        sb.append(", rsHost='").append(rsHost).append('\'');
        sb.append(", resourceSetId='").append(resourceSetId).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append('}');
        return sb.toString();
    }
}
