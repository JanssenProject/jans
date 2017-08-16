/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.session.EndSessionRequestParam;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Represents an end session request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class EndSessionRequest extends BaseRequest {

    private String idTokenHint;
    private String postLogoutRedirectUri;
    private String sessionId;
    private String state;

    /**
     * Constructs an end session request.
     */
    public EndSessionRequest(String idTokenHint, String postLogoutRedirectUri, String state) {
        this.idTokenHint = idTokenHint;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        this.state = state;
    }

    /**
     * Returns the issued ID Token.
     *
     * @return The issued ID Token.
     */
    public String getIdTokenHint() {
        return idTokenHint;
    }

    /**
     * Sets the issued ID Token.
     *
     * @param idTokenHint The issued ID Token.
     */
    public void setAccessToken(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    /**
     * Returns the URL to which the RP is requesting that the End-User's User-Agent be redirected after a logout
     * has been performed.
     *
     * @return The post logout redirection URI.
     */
    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    /**
     * Sets the URL to which the RP is requesting that the End-User's User-Agent be redirected after a logout
     * has been performed.
     *
     * @param postLogoutRedirectUri The post logout redirection URI.
     */
    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    /**
     * Gets session id.
     *
     * @return session id.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets session id.
     *
     * @param p_sessionId session id
     */
    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
    }

    /**
     * Returns the state. The state is an opaque value used by the RP to maintain state between the logout request and
     * the callback to the endpoint specified by the post_logout_redirect_uri parameter. If included in the logout
     * request, the OP passes this value back to the RP using the state query parameter when redirecting the User Agent
     * back to the RP.
     *
     * @return The state.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state. The state is an opaque value used by the RP to maintain state between the logout request and the
     * callback to the endpoint specified by the post_logout_redirect_uri parameter. If included in the logout request,
     * the OP passes this value back to the RP using the state query parameter when redirecting the User Agent back to
     * the RP.
     *
     * @param state he state.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns a query string with the parameters of the end session request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            if (StringUtils.isNotBlank(idTokenHint)) {
                queryStringBuilder.append(EndSessionRequestParam.ID_TOKEN_HINT)
                        .append("=")
                        .append(idTokenHint);
            }
            if (StringUtils.isNotBlank(postLogoutRedirectUri)) {
                queryStringBuilder.append("&")
                        .append(EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI)
                        .append("=")
                        .append(URLEncoder.encode(postLogoutRedirectUri, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(state)) {
                queryStringBuilder.append("&")
                        .append(EndSessionRequestParam.STATE)
                        .append("=")
                        .append(URLEncoder.encode(state, Util.UTF8_STRING_ENCODING));
            }

            if (StringUtils.isNotBlank(sessionId)) {
                queryStringBuilder.append("&")
                        .append(EndSessionRequestParam.SESSION_ID)
                        .append("=")
                        .append(URLEncoder.encode(sessionId, Util.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}