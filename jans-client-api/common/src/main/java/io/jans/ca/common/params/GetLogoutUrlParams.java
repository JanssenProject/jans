package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetLogoutUrlParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;

    @JsonProperty(value = "id_token_hint")
    private String id_token_hint;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String post_logout_redirect_uri;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "session_state")
    private String session_state;

    public GetLogoutUrlParams() {
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

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
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
        sb.append("{rp_id=").append(rp_id);
        sb.append(", id_token_hint=").append(id_token_hint);
        sb.append(", post_logout_redirect_uri=").append(post_logout_redirect_uri);
        sb.append(", state=").append(state);
        sb.append(", session_state=").append(session_state);
        sb.append('}');
        return sb.toString();
    }
}
