package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class LogoutParams implements IParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    @JsonProperty(value = "id_token")
    private String idToken;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String postLogoutRedirectUri;
    @JsonProperty(value = "http_based_logout")
    private boolean httpBasedLogout = false;

    public LogoutParams() {
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public boolean isHttpBasedLogout() {
        return httpBasedLogout;
    }

    public void setHttpBasedLogout(boolean httpBasedLogout) {
        this.httpBasedLogout = httpBasedLogout;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogoutParams");
        sb.append("{httpBasedLogout=").append(httpBasedLogout);
        sb.append("{oxdId=").append(oxdId);
        sb.append("{idToken=").append(idToken);
        sb.append("{postLogoutRedirectUri=").append(postLogoutRedirectUri);
        sb.append('}');
        return sb.toString();
    }
}
