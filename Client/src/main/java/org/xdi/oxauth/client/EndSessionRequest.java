/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.common.Parameters;
import org.xdi.oxauth.model.session.EndSessionRequestParam;

/**
 * Represents an end session request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 12.14.2011
 */
public class EndSessionRequest extends BaseRequest {

    private String idTokenHint;
    private String postLogoutRedirectUri;
    private String sessionId;

    /**
     * Constructs an end session request.
     */
    public EndSessionRequest(String idTokenHint, String postLogoutRedirectUri) {
        this.idTokenHint = idTokenHint;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
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
     * @return session id
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
                        .append(URLEncoder.encode(postLogoutRedirectUri, "UTF-8"));
            }

            if (StringUtils.isNotBlank(sessionId)) {
                queryStringBuilder.append(Parameters.SESSION_ID.nameToAppend()).append(sessionId);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}