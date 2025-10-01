/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.TokenTypeHint;
import io.jans.as.model.token.TokenRevocationRequestParam;
import io.jans.as.model.util.QueryBuilder;
import org.apache.log4j.Logger;

import jakarta.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationRequest extends ClientAuthnRequest {

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
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.append(TokenRevocationRequestParam.TOKEN, token);
        queryBuilder.append(TokenRevocationRequestParam.TOKEN_TYPE_HINT, tokenTypeHint != null ? tokenTypeHint.toString() : null);
        queryBuilder.append("client_id", getAuthUsername());
        return queryBuilder.toString();
    }
}
