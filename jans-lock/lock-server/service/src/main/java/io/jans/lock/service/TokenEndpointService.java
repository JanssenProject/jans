package io.jans.lock.service;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.lock.model.AuditEndpointType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.model.net.HttpServiceResponse;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class TokenEndpointService {

    @Inject
    Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private OpenIdService openIdService;

    @Inject
    private EncryptionService encryptionService;

    /**
     * Obtain an access token appropriate for the given audit endpoint type.
     *
     * @param requestType the audit endpoint type used to derive the OAuth/OpenID scopes for the token request
     * @return a Token containing the access token, scopes, and expiry when successful; `null` if a token could not be obtained
     */
    public Token getAccessToken(AuditEndpointType requestType) {
        log.debug("Request for token for requestType: {}", requestType);

        String clientId = this.appConfiguration.getClientId();
        String clientSecret = this.getDecryptedPassword(appConfiguration.getClientPassword());
        String scopes = this.getScopesForToken(requestType);

        log.debug("Scopes for requestType: {}, scopes:{}", requestType, scopes);

        return getToken(openIdService.getOpenIdConfiguration().getTokenEndpoint(), clientId, clientSecret, scopes);
    }

    /**
     * Obtain an access token from the specified token endpoint using client credentials and scopes.
     *
     * @param tokenUrl     the token endpoint URL
     * @param clientId     the OAuth client identifier
     * @param clientSecret the client's secret (decrypted)
     * @param scopes       space-separated OAuth/OpenID scopes to request
     * @return             a Token containing the access token and OPENID scope if obtained, `null` otherwise
     */
    public Token getToken(String tokenUrl, String clientId, String clientSecret, String scopes) {
        log.debug("Request for token tokenUrl:{}, clientId:{},scopes:{}", tokenUrl, clientId, scopes);
        Token token = null;
        TokenResponse tokenResponse = requestAccessToken(tokenUrl, clientId, clientSecret, scopes);
        if (tokenResponse != null) {
            final String accessToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            log.trace("accessToken:{}, expiresIn:{}", accessToken, expiresIn);
            if (Util.allNotBlank(accessToken)) {
                return new Token(null, null, accessToken, ScopeType.OPENID.getValue(), expiresIn);
            }
        }

        return token;
    }

    /**
     * Request an access token from the token endpoint using the OAuth2 client credentials grant.
     *
     * @param tokenUrl the token endpoint URL
     * @param clientId the OAuth2 client identifier
     * @param clientSecret the client's secret (decrypted)
     * @param scope the space-separated OAuth2 scopes to request
     * @return the TokenResponse when the endpoint returns HTTP 200, or `null` if the request failed
     */
    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope) {
        log.debug("Request for access token tokenUrl:{}, clientId:{},scope:{}", tokenUrl, clientId, scope);
        TokenClient tokenClient = new TokenClient(tokenUrl);
        TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scope, clientId, clientSecret);
        
        log.trace("Response for Access Token -  tokenResponse: {}", tokenResponse);
        if ((tokenResponse == null) || (tokenResponse.getStatus() != HttpStatus.SC_OK)) {
            log.error("Failed to get access token with scopes: {}", scope);
            return null;
        }

        return tokenResponse;
    }

    /**
     * Map an HttpServiceResponse's HTTP status code to a JAX-RS Status.
     *
     * @param serviceResponse the service response containing the HTTP response; may be null
     * @return the corresponding JAX-RS {@link javax.ws.rs.core.Response.Status}, or {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} if the response is null or no matching status exists
     */
    public Status getResponseStatus(HttpServiceResponse serviceResponse) {
        Status status = Status.INTERNAL_SERVER_ERROR;

        if (serviceResponse == null || serviceResponse.getHttpResponse() == null) {
            return status;
        }

        int statusCode = serviceResponse.getHttpResponse().getStatusLine().getStatusCode();
        status = Status.fromStatusCode(statusCode);
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }

        return status;
    }

    /**
     * Decrypts an encrypted client password.
     *
     * @param clientPassword the encrypted password to decrypt, or {@code null}
     * @return the decrypted password, or {@code null} if {@code clientPassword} is {@code null} or decryption fails
     */
    public String getDecryptedPassword(String clientPassword) {
        String decryptedPassword = null;
        if (clientPassword != null) {
            try {
                decryptedPassword = encryptionService.decrypt(clientPassword);
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt password", ex);
            }
        }
        return decryptedPassword;
    }

    /**
     * Builds the OAuth/OpenID scope string required for a token request for the given audit endpoint type.
     *
     * @param requestType the audit endpoint type whose scopes should be included
     * @return a space-separated scope string beginning with "openid" followed by the scopes from {@code requestType}
     */
    private String getScopesForToken(AuditEndpointType requestType) {
        log.debug("Build scopes for requestType: {}", requestType);
        StringBuilder sb = new StringBuilder();
        sb.append(ScopeType.OPENID.getValue());
        for (String scope : requestType.getScopes()) {
            sb.append(" ").append(scope);
        }

        return sb.toString();
    }

}