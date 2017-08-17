package org.xdi.oxd.common.params;

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
    private String oxdId;
    @JsonProperty(value = "acr_values")
    private List<String> acrValues;
    @JsonProperty(value = "prompt")
    private String prompt;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "hd")
    private String hostedDomain; // https://developers.google.com/identity/protocols/OpenIDConnect#hd-param
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;
    @JsonProperty(value = "custom_parameters")
    private Map<String, String> customParameters;

    public GetAuthorizationUrlParams() {
    }

    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.customParameters = customParameters;
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    public String getHostedDomain() {
        return hostedDomain;
    }

    public void setHostedDomain(String hostedDomain) {
        this.hostedDomain = hostedDomain;
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
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetAuthorizationUrlParams that = (GetAuthorizationUrlParams) o;

        return !(acrValues != null ? !acrValues.equals(that.acrValues) : that.acrValues != null) && !(oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null);

    }

    @Override
    public int hashCode() {
        int result = oxdId != null ? oxdId.hashCode() : 0;
        result = 31 * result + (acrValues != null ? acrValues.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GetAuthorizationUrlParams{" +
                "oxdId='" + oxdId + '\'' +
                ", acrValues=" + acrValues +
                ", prompt='" + prompt + '\'' +
                ", scope=" + scope +
                ", hostedDomain='" + hostedDomain + '\'' +
                ", protectionAccessToken='" + protectionAccessToken + '\'' +
                ", customParameters=" + customParameters +
                '}';
    }
}
