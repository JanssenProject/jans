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

    public Token getAccessToken(AuditEndpointType requestType) {
        log.debug("Request for token for requestType: {}", requestType);

        String clientId = this.appConfiguration.getClientId();
        String clientSecret = this.getDecryptedPassword(appConfiguration.getClientPassword());
        String scopes = this.getScopesForToken(requestType);

        log.debug("Scopes for requestType: {}, scopes:{}", requestType, scopes);

        return getToken(openIdService.getOpenIdConfiguration().getTokenEndpoint(), clientId, clientSecret, scopes);
    }

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

    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            final String scope) {
        log.debug("Request for access token tokenUrl:{}, clientId:{},scope:{}", tokenUrl, clientId, scope);
        TokenClient tokenClient = new TokenClient(tokenUrl);
        TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scope, clientId, clientSecret);
        
        log.trace("Response for Access Token -  tokenResponse: {}", tokenResponse);
        if ((tokenResponse == null) || (tokenResponse.getStatus() != HttpStatus.SC_OK)) {
            log.error("Failed to get acces token with scopes: ", scope);
            return null;
        }

        return tokenResponse;
    }

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
