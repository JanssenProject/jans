/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.TokenTypeHint;
import org.gluu.oxauth.model.token.TokenRevocationRequestParam;

import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationRequest extends BaseRequest {

    private static final Logger LOG = Logger.getLogger(TokenRevocationRequest.class);

    private String token;
    private TokenTypeHint tokenTypeHint;

    /**
     * Constructs a token revocation request.
     */
    public TokenRevocationRequest() {
        super();

        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenTypeHint getTokenTypeHint() {
        return tokenTypeHint;
    }

    public void setTokenTypeHint(TokenTypeHint tokenTypeHint) {
        this.tokenTypeHint = tokenTypeHint;
    }

    /**
     * Returns a query string with the parameters of the toke revocation request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            if (StringUtils.isNotBlank(token)) {
                queryStringBuilder.append(TokenRevocationRequestParam.TOKEN)
                        .append("=").append(token);
            }
            if (tokenTypeHint != null) {
                queryStringBuilder.append("&");
                queryStringBuilder.append(TokenRevocationRequestParam.TOKEN_TYPE_HINT)
                        .append("=")
                        .append(tokenTypeHint.toString());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return queryStringBuilder.toString();
    }
}
