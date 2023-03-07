package io.jans.ca.plugin.adminui.service;

import com.google.common.base.Strings;
import io.jans.as.client.TokenRequest;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

public class BaseService {

    @Inject
    Logger log;

    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint) {
        return getToken(tokenRequest, tokenEndpoint, null);
    }

    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt) {

        try {
            MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
            if (!Strings.isNullOrEmpty(tokenRequest.getCode())) {
                body.putSingle("code", tokenRequest.getCode());
            }

            if (!Strings.isNullOrEmpty(tokenRequest.getScope())) {
                body.putSingle("scope", tokenRequest.getScope());
            }

            if (!Strings.isNullOrEmpty(userInfoJwt)) {
                body.putSingle("ujwt", userInfoJwt);
            }

            body.putSingle("grant_type", tokenRequest.getGrantType().getValue());
            body.putSingle("redirect_uri", tokenRequest.getRedirectUri());
            body.putSingle("client_id", tokenRequest.getAuthUsername());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(tokenEndpoint);
            Response response = request
                    .header("Authorization", "Basic " + tokenRequest.getEncodedCredentials())
                    .post(Entity.form(body));

            log.info("Get Access Token status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);

                io.jans.as.client.TokenResponse tokenResponse = new io.jans.as.client.TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);

                return tokenResponse;
            }
            log.error("Error in getting access token: {}", response.getEntity());

        } catch (Exception e) {
            log.error("Problems processing token call");
            throw e;

        }
        return null;
    }
}
