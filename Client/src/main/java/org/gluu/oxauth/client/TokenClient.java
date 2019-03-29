/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.token.ClientAssertionType;

import javax.ws.rs.HttpMethod;

/**
 * Encapsulates functionality to make token request calls to an authorization
 * server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version June 28, 2017
 */
public class TokenClient extends BaseClient<TokenRequest, TokenResponse> {

    private static final Logger LOG = Logger.getLogger(TokenClient.class);

    /**
     * Constructs a token client by providing a REST url where the token service
     * is located.
     *
     * @param url The REST Service location.
     */
    public TokenClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    /**
     * <p>
     * Executes the call to the REST Service requesting the authorization and
     * processes the response.
     * </p>
     * <p>
     * The authorization code is obtained by using an authorization server as an
     * intermediary between the client and resource owner. Instead of requesting
     * authorization directly from the resource owner, the client directs the
     * resource owner to an authorization server (via its user- agent as defined in
     * [RFC2616]), which in turn directs the resource owner back to the client with
     * the authorization code.
     * </p>
     * <p>
     * Before directing the resource owner back to the client with the authorization
     * code, the authorization server authenticates the resource owner and obtains
     * authorization. Because the resource owner only authenticates with the
     * authorization server, the resource owner's credentials are never shared with
     * the client.
     * </p>
     * <p>
     * The authorization code provides a few important security benefits such as the
     * ability to authenticate the client, and the transmission of the access token
     * directly to the client without passing it through the resource owner's
     * user-agent, potentially exposing it to others, including the resource owner.
     * </p>
     *
     * @param code         he authorization code received from the authorization server.
     *                     This parameter is required.
     * @param redirectUri  The redirection URI. This parameter is required.
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return The token response.
     */
    public TokenResponse execAuthorizationCode(String code, String redirectUri,
                                               String clientId, String clientSecret) {
        setRequest(new TokenRequest(GrantType.AUTHORIZATION_CODE));
        getRequest().setCode(code);
        getRequest().setRedirectUri(redirectUri);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * <p>
     * Executes the call to the REST Service requesting the authorization and
     * processes the response.
     * </p>
     * <p>
     * The resource owner password credentials grant type is suitable in cases
     * where the resource owner has a trust relationship with the client, such
     * as its device operating system or a highly privileged application. The
     * authorization server should take special care when enabling this grant
     * type, and only allow it when other flows are not viable.
     * </p>
     * <p>
     * The grant type is suitable for clients capable of obtaining the resource
     * owner's credentials (username and password, typically using an
     * interactive form). It is also used to migrate existing clients using
     * direct authentication schemes such as HTTP Basic or Digest authentication
     * to OAuth by converting the stored credentials to an access token.
     * </p>
     *
     * @param username     The resource owner username. This parameter is required.
     * @param password     The resource owner password. This parameter is required.
     * @param scope        The scope of the access request. This parameter is optional.
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return The token response.
     */
    public TokenResponse execResourceOwnerPasswordCredentialsGrant(
            String username, String password, String scope,
            String clientId, String clientSecret) {
        setRequest(new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS));
        getRequest().setUsername(username);
        getRequest().setPassword(password);
        getRequest().setScope(scope);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * <p>
     * Executes the call to the REST Service requesting the authorization and
     * processes the response.
     * </p>
     * <p>
     * The client can request an access token using only its client credentials
     * when the client is requesting access to the protected resources under its
     * control, or those of another resource owner which has been previously
     * arranged with the authorization server. The client credentials grant type
     * must only be used by confidential clients.
     * </p>
     *
     * @param scope        The scope of the access request. This parameter is optional.
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return The token response.
     */
    public TokenResponse execClientCredentialsGrant(
            String scope, String clientId, String clientSecret) {
        setRequest(new TokenRequest(GrantType.CLIENT_CREDENTIALS));
        getRequest().setScope(scope);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * <p>
     * Executes the call to the REST Service requesting the authorization and
     * processes the response.
     * </p>
     * <p>
     * The client uses an extension grant type by specifying the grant type
     * using an absolute URI (defined by the authorization server) as the value
     * of the grant_type parameter of the token endpoint, and by adding any
     * additional parameters necessary.
     * </p>
     *
     * @param grantTypeUri Absolute URI.
     * @param assertion    Assertion grant type.
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return The token response.
     */
    public TokenResponse execExtensionGrant(String grantTypeUri, String assertion,
                                            String clientId, String clientSecret) {
        GrantType grantType = GrantType.fromString(grantTypeUri);
        setRequest(new TokenRequest(grantType));
        getRequest().setAssertion(assertion);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * <p>
     * Executes the call to the REST Service requesting the authorization and
     * processes the response.
     * </p>
     * <p>
     * If the authorization server issued a refresh token to the client, the
     * client can make a request to the token endpoint for a new access token.
     * </p>
     *
     * @param scope        The scope of the access request. This value is optional.
     * @param refreshToken The refresh token issued to the client. This value is
     *                     required.
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return The token response.
     */
    public TokenResponse execRefreshToken(String scope, String refreshToken,
                                          String clientId, String clientSecret) {
        setRequest(new TokenRequest(GrantType.REFRESH_TOKEN));
        getRequest().setScope(scope);
        getRequest().setRefreshToken(refreshToken);
        getRequest().setAuthUsername(clientId);
        getRequest().setAuthPassword(clientSecret);

        return exec();
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The token response.
     */
    public TokenResponse exec() {
        // Prepare request parameters
        initClientRequest();
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC
                && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
        }
        clientRequest.header("Content-Type", request.getContentType());
        clientRequest.setHttpMethod(getHttpMethod());

        if (getRequest().getGrantType() != null) {
            clientRequest.formParameter("grant_type", getRequest().getGrantType());
        }
        if (StringUtils.isNotBlank(getRequest().getCode())) {
            clientRequest.formParameter("code", getRequest().getCode());
        }
        if (StringUtils.isNotBlank(getRequest().getCodeVerifier())) {
            clientRequest.formParameter("code_verifier", getRequest().getCodeVerifier());
        }
        if (StringUtils.isNotBlank(getRequest().getRedirectUri())) {
            clientRequest.formParameter("redirect_uri", getRequest().getRedirectUri());
        }
        if (StringUtils.isNotBlank(getRequest().getUsername())) {
            clientRequest.formParameter("username", getRequest().getUsername());
        }
        if (StringUtils.isNotBlank(getRequest().getPassword())) {
            clientRequest.formParameter("password", getRequest().getPassword());
        }
        if (StringUtils.isNotBlank(getRequest().getScope())) {
            clientRequest.formParameter("scope", getRequest().getScope());
        }
        if (StringUtils.isNotBlank(getRequest().getAssertion())) {
            clientRequest.formParameter("assertion", getRequest().getAssertion());
        }
        if (StringUtils.isNotBlank(getRequest().getRefreshToken())) {
            clientRequest.formParameter("refresh_token", getRequest().getRefreshToken());
        }
        if (getRequest().getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (getRequest().getAuthUsername() != null && !getRequest().getAuthUsername().isEmpty()) {
                clientRequest.formParameter("client_id", getRequest().getAuthUsername());
            }
            if (getRequest().getAuthPassword() != null && !getRequest().getAuthPassword().isEmpty()) {
                clientRequest.formParameter("client_secret", getRequest().getAuthPassword());
            }
        } else if (getRequest().getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                getRequest().getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            clientRequest.formParameter("client_assertion_type", ClientAssertionType.JWT_BEARER);
            clientRequest.formParameter("client_assertion", getRequest().getClientAssertion());
            if (getRequest().getAuthUsername() != null && !getRequest().getAuthUsername().isEmpty()) {
                clientRequest.formParameter("client_id", getRequest().getAuthUsername());
            }
        }
        for (String key : getRequest().getCustomParameters().keySet()) {
            clientRequest.formParameter(key, getRequest().getCustomParameters().get(key));
        }

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.post(String.class);

            final TokenResponse tokenResponse = new TokenResponse(clientResponse);
            tokenResponse.injectDataFromJson();
            setResponse(tokenResponse);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}