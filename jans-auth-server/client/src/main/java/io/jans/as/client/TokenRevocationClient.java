/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.TokenTypeHint;
import io.jans.as.model.token.TokenRevocationRequestParam;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * Encapsulates functionality to make token revocation request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationClient extends BaseClient<TokenRevocationRequest, TokenRevocationResponse> {

    private static final Logger LOG = Logger.getLogger(TokenRevocationClient.class);

    /**
     * Constructs a token revocation client by providing a REST url where the token service is located.
     *
     * @param url The REST Service location.
     */
    public TokenRevocationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    /**
     * Executes the call to the REST Service requesting the token revocation and processes the response.
     *
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @param token        The token that the client wants to get revoked.
     * @return The token revocation response.
     */
    public TokenRevocationResponse execTokenRevocation(String clientId, String clientSecret, String token) {
        return execTokenRevocation(clientId, clientSecret, token, null);
    }

    /**
     * Executes the call to the REST Service requesting the token revocation and processes the response.
     *
     * @param clientId      The client identifier.
     * @param clientSecret  The client secret.
     * @param token         The token that the client wants to get revoked.
     * @param tokenTypeHint A hint about the type of the token submitted for revocation.
     * @return The token revocation response.
     */
    public TokenRevocationResponse execTokenRevocation(String clientId, String clientSecret, String token, TokenTypeHint tokenTypeHint) {
        setRequest(new TokenRevocationRequest());
        getRequest().setToken(token);
        getRequest().setTokenTypeHint(tokenTypeHint);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The token revocation response.
     */
    public TokenRevocationResponse exec() {
        // Prepare request parameters
        initClient();

        if (StringUtils.isNotBlank(getRequest().getToken())) {
            requestForm.param(TokenRevocationRequestParam.TOKEN, getRequest().getToken());
        }
        if (getRequest().getTokenTypeHint() != null) {
            requestForm.param(TokenRevocationRequestParam.TOKEN_TYPE_HINT, getRequest().getTokenTypeHint().toString());
        }
        if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
            requestForm.param("client_id", request.getAuthUsername());
        }

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        new ClientAuthnEnabler(clientRequest, requestForm).exec(request);

        clientRequest.header("Content-Type", request.getContentType());

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();

            final TokenRevocationResponse tokenResponse = new TokenRevocationResponse(clientResponse);
            setResponse(tokenResponse);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
