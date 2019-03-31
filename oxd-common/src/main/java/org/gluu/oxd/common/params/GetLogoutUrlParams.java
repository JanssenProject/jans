package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetLogoutUrlParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;

    @JsonProperty(value = "id_token_hint")
    private String id_token_hint;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String post_logout_redirect_uri;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "session_state")
    private String session_state;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public GetLogoutUrlParams() {
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getPostLogoutRedirectUri() {
        return post_logout_redirect_uri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.post_logout_redirect_uri = postLogoutRedirectUri;
    }

    public String getIdTokenHint() {
        return id_token_hint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.id_token_hint = idTokenHint;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSessionState() {
        return session_state;
    }

    public void setSessionState(String sessionState) {
        this.session_state = sessionState;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogoutParams");
        sb.append("{oxd_id=").append(oxd_id);
        sb.append(", id_token_hint=").append(id_token_hint);
        sb.append(", post_logout_redirect_uri=").append(post_logout_redirect_uri);
        sb.append(", state=").append(state);
        sb.append(", session_state=").append(session_state);
        sb.append(", protection_access_token=").append(protection_access_token);
        sb.append('}');
        return sb.toString();
    }
}
