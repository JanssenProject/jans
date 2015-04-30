package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RegisterResourceParams implements IParams {

    @JsonProperty(value = "uma_discovery_url")
    private String umaDiscoveryUrl;
    @JsonProperty(value = "pat")
    private String patToken;
    @JsonProperty(value = "name")
    private String name;
    @JsonProperty(value = "scopes")
    private List<String> scopes;

    public RegisterResourceParams() {
    }

    public String getPatToken() {
        return patToken;
    }

    public void setPatToken(String p_patToken) {
        patToken = p_patToken;
    }

    public String getUmaDiscoveryUrl() {
        return umaDiscoveryUrl;
    }

    public void setUmaDiscoveryUrl(String p_umaDiscoveryUrl) {
        umaDiscoveryUrl = p_umaDiscoveryUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String p_name) {
        name = p_name;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> p_scopes) {
        scopes = p_scopes;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterResourceParams");
        sb.append("{umaDiscoveryUrl='").append(umaDiscoveryUrl).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append('}');
        return sb.toString();
    }
}
