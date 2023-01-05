package io.jans.inbound.oauth2;

import java.util.List;
import java.util.Map;

public class OAuthParams {
    
    private String authzEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;

    private String clientId;
    private String clientSecret;
    private List<String> scopes;

    private String redirectUri;
    
    private boolean clientCredsInRequestBody;
    private Map<String, String> custParamsAuthReq;
    private Map<String, String> custParamsTokenReq;

    public String getAuthzEndpoint() {
        return authzEndpoint;
    }

    public void setAuthzEndpoint(String authzEndpoint) {
        this.authzEndpoint = authzEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
    
    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }
    
    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
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

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public boolean isClientCredsInRequestBody() {
        return clientCredsInRequestBody;
    }
    
    public void setClientCredsInRequestBody(boolean clientCredsInRequestBody) {
        this.clientCredsInRequestBody = clientCredsInRequestBody;
    }
    
    public Map<String, String> getCustParamsAuthReq() {
        return custParamsAuthReq;
    }
    
    public void setCustParamsAuthReq(Map<String, String> custParamsAuthReq) {
        this.custParamsAuthReq = custParamsAuthReq;
    }
    
    public Map<String, String> getCustParamsTokenReq() {
        return custParamsTokenReq;
    }
    
    public void setCustParamsTokenReq(Map<String, String> custParamsTokenReq) {
        this.custParamsTokenReq = custParamsTokenReq;
    }
}