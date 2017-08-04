package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetLogoutUrlParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    @JsonProperty(value = "id_token_hint")
    private String idTokenHint;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String postLogoutRedirectUri;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "session_state")
    private String sessionState;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    public GetLogoutUrlParams() {
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogoutParams");
        sb.append("{oxdId=").append(oxdId);
        sb.append(", idTokenHint=").append(idTokenHint);
        sb.append(", postLogoutRedirectUri=").append(postLogoutRedirectUri);
        sb.append(", state=").append(state);
        sb.append(", sessionState=").append(sessionState);
        sb.append(", protectionAccessToken=").append(protectionAccessToken);
        sb.append('}');
        return sb.toString();
    }
}
