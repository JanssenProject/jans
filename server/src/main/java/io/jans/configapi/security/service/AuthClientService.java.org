/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwk.JSONWebKeySet;
import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import io.jans.configapi.util.Jackson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClientService {

    private static Logger log = LoggerFactory.getLogger(AuthClientService.class);

    public static String getIntrospectionEndpoint(String issuer) throws Exception {
        log.debug("\n\n AuthClientService::getIntrospectionEndpoint() - issuer = " + issuer);

        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        Builder request = ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();

        log.debug("\n\n AuthClientService::getIntrospectionEndpoint() - configurationEndpoint = "
                + configurationEndpoint + " ,response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getIntrospectionEndpoint() - entity = " + entity);
            return Jackson.getElement(entity, "introspection_endpoint");
        }
        return null;
    }

    public static IntrospectionResponse getIntrospectionResponse(String url, String header, String token,
            boolean followRedirects) {
        log.debug("\n\n AuthClientService:::getIntrospectionResponse() - url = " + url + " , header = " + header
                + " , token = " + token + " , followRedirects = " + followRedirects);

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(url);
        return introspectionService.introspectToken(header, token);

    }

    public static String getJwksUri(String issuer) throws Exception {
        log.debug("\n\n AuthClientService::getJwksUri() - issuer = " + issuer);

        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        Builder request = ResteasyClientBuilder.newClient().target(configurationEndpoint).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();

        log.debug("\n\n AuthClientService::getJwksUri() - configurationEndpoint =" + configurationEndpoint
                + " , response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getJwksUri() - entity = " + entity);
            return Jackson.getElement(entity, "jwks_uri");
        }
        return null;
    }

    public static JSONWebKeySet getJSONWebKeys(String jwksUri) throws Exception {
        log.trace("\n\n AuthClientService::getJSONWebKeys() - jwksUri = " + jwksUri);

        Builder request = ResteasyClientBuilder.newClient().target(jwksUri).request();
        request.header("Content-Type", MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.trace("\n\n AuthClientService::getJSONWebKeys() - response = " + response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientService::getJSONWebKeys() - entity = " + entity);
            JwkResponse jwkResponse = new JwkResponse(200);
            JSONWebKeySet jwks = null;
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(JSON_WEB_KEY_SET)) {
                    jwks = JSONWebKeySet.fromJSONObject(jsonObj);
                    jwkResponse.setJwks(jwks);
                }
                log.trace("\n\n AuthClientService::getJSONWebKeys() - jwkResponse = " + jwkResponse + " , jwks = "
                        + jwks);
                return jwks;
            }

        }
        return null;
    }

    public static TokenResponse requestAccessToken(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {

        log.debug("\n\n AuthClientService::requestAccessToken() - tokenUrl = " + tokenUrl + " , clientId = " + clientId
                + " , clientSecret = " + clientSecret + " , scope = " + scope);

        Builder request = ResteasyClientBuilder.newClient().target(tokenUrl).request();
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope(scope);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        TokenClient tokenClient = new TokenClient(tokenUrl);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        log.debug("\n\n AuthClientService::requestAccessToken() - tokenResponse = " + tokenResponse);

        if (tokenResponse.getStatus() == 200) {

            return tokenResponse;
        }

        return null;
    }

}
