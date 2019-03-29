package org.gluu.model.passport.idpinitiated;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by jgomer on 2019-02-21.
 */
public class AuthzParams {

    private String provider;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("scope")
    private String scopes;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

}
