package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DCRequest {
    @SerializedName("issuer")
    private String issuer;
    @SerializedName("redirect_uris")
    private List<String> redirectUris;
    @SerializedName("scope")
    private String scope;
    @SerializedName("response_types")
    private List<String> responseTypes;
    @SerializedName("post_logout_redirect_uris")
    private List<String> postLogoutRedirectUris;
    @SerializedName("grant_types")
    private List<String> grantTypes;
    @SerializedName("application_type")
    private String applicationType;
    @SerializedName("client_name")
    private String clientName;
    @SerializedName("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;
    @SerializedName("evidence")
    private String evidence;
    @SerializedName("jwks")
    private String jwks;

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    @Override
    public String toString() {
        return "DCRequest{" +
                "issuer='" + issuer + '\'' +
                ", redirectUris=" + redirectUris +
                ", scope='" + scope + '\'' +
                ", responseTypes=" + responseTypes +
                ", postLogoutRedirectUris=" + postLogoutRedirectUris +
                ", grantTypes=" + grantTypes +
                ", applicationType='" + applicationType + '\'' +
                ", clientName='" + clientName + '\'' +
                ", tokenEndpointAuthMethod='" + tokenEndpointAuthMethod + '\'' +
                ", evidence='" + evidence + '\'' +
                '}';
    }
}
