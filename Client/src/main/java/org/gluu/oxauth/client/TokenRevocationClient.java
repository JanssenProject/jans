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

import javax.ws.rs.HttpMethod;

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
        initClientRequest();
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC
                && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
        }
        clientRequest.header("Content-Type", request.getContentType());
        clientRequest.setHttpMethod(getHttpMethod());

        if (StringUtils.isNotBlank(getRequest().getToken())) {
            clientRequest.formParameter(TokenRevocationRequestParam.TOKEN, getRequest().getToken());
        }
        if (getRequest().getTokenTypeHint() != null) {
            clientRequest.formParameter(TokenRevocationRequestParam.TOKEN_TYPE_HINT, getRequest().getTokenTypeHint());
        }

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.post(String.class);

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
