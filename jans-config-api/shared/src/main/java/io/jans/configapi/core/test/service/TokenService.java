/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test.service;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.configapi.core.test.service.TokenService;


import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

public class TokenService implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient Logger log = LogManager.getLogger(getClass());
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    
    public String getToken(final String tokenUrl, final String clientId, final String clientSecret, GrantType grantType,
            final String scopes) {
        log.info("Request for token tokenUrl:{}, clientId:{}, grantType:{}, scopes:{}", tokenUrl, clientId, grantType,
                scopes);
        String accessToken = null;
        TokenResponse tokenResponse = this.requestAccessToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        if (tokenResponse != null) {
            accessToken = tokenResponse.getAccessToken();
            log.trace("accessToken:{}, ", accessToken);
        }

        return accessToken;
    }

    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId, final String clientSecret,
            GrantType grantType, final String scope) {
        log.info("Request for access token tokenUrl:{}, clientId:{},scope:{}", tokenUrl, clientId, scope);
        Response response = null;
        try {
            if (grantType == null) {
                grantType = GrantType.CLIENT_CREDENTIALS;
            }
            TokenRequest tokenRequest = new TokenRequest(grantType);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            Builder request = new ResteasyService().getClientBuilder(tokenUrl);
            request.header(AUTHORIZATION, "Basic " + tokenRequest.getEncodedCredentials());
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>(
                    tokenRequest.getParameters());
            response = request.post(Entity.form(multivaluedHashMap));
            log.trace("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);
                return tokenResponse;
            }
        } finally {

            if (response != null) {
                response.close();
            }
        }
        return null;
    }

}
