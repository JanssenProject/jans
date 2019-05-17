package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAuthorizationUrlParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "acr_values")
    private List<String> acr_values;
    @JsonProperty(value = "prompt")
    private String prompt;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "hd")
    private String hd; // https://developers.google.com/identity/protocols/OpenIDConnect#hd-param
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;
    @JsonProperty(value = "custom_parameters")
    private Map<String, String> custom_parameters;
    @JsonProperty(value = "authorization_redirect_uri")
    private String authorization_redirect_uri;

    public GetAuthorizationUrlParams() {
    }

    public Map<String, String> getCustomParameters() {
        return custom_parameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.custom_parameters = customParameters;
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getHostedDomain() {
        return hd;
    }

    public void setHostedDomain(String hostedDomain) {
        this.hd = hostedDomain;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public List<String> getAcrValues() {
        return acr_values;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acr_values = acrValues;
    }

    public String getAuthorizationRedirectUri() {
        return authorization_redirect_uri;
    }

    public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
        this.authorization_redirect_uri = authorizationRedirectUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetAuthorizationUrlParams that = (GetAuthorizationUrlParams) o;

        return !(acr_values != null ? !acr_values.equals(that.acr_values) : that.acr_values != null) && !(oxd_id != null ? !oxd_id.equals(that.oxd_id) : that.oxd_id != null);

    }

    @Override
    public int hashCode() {
        int result = oxd_id != null ? oxd_id.hashCode() : 0;
        result = 31 * result + (acr_values != null ? acr_values.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GetAuthorizationUrlParams{" +
                "oxd_id='" + oxd_id + '\'' +
                ", acr_values=" + acr_values +
                ", prompt='" + prompt + '\'' +
                ", scope=" + scope +
                ", hd='" + hd + '\'' +
                ", protection_access_token='" + protection_access_token + '\'' +
                ", custom_parameters=" + custom_parameters +
                ", authorization_redirect_uri=" + authorization_redirect_uri +
                '}';
    }
}
