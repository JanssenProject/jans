/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.client;

import io.jans.as.client.service.StatService;
import io.jans.as.client.JwkResponse;
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

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

@RegisterProvider(OpenIdClientService.class)
public class AuthClientFactory {

    private static final String CONTENT_TYPE = "Content-Type";

    @Inject
    @RestClient
    OpenIdClientService openIdClientService;

    @Inject
    @RestClient
    StatClient statClient;

    private static Logger log = LoggerFactory.getLogger(AuthClientFactory.class);

    public static IntrospectionService getIntrospectionService(String url, boolean followRedirects) {
        return createIntrospectionService(url, followRedirects);
    }

    public static IntrospectionResponse getIntrospectionResponse(String url, String header, String token,
            boolean followRedirects) {

        log.info("AuthClientFactory - getIntrospectionResponse() - url:{}, header:{}, token:{} ,followRedirects:{} ",
                url, header, token, followRedirects);

        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build());
        ResteasyWebTarget target = (ResteasyWebTarget) ClientBuilder.newClient(restClient.getConfiguration())
                .property(CONTENT_TYPE, MediaType.APPLICATION_JSON).target(url);

        IntrospectionService proxy = target.proxy(IntrospectionService.class);
        return proxy.introspectToken(header, token);

    }

    public static JsonNode getStatResponse(String url, String token, String month, String format) {
        log.debug("Stat Report - url:{}, token:{}, month:{}, format:{}", url, token, month, format);

        Builder request = ClientBuilder.newClient().target(url).request();
        request.header("Authorization", "Basic " + token);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
        multivaluedHashMap.add("month", month);
        multivaluedHashMap.add("format", format);

        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build());
        ResteasyWebTarget target = (ResteasyWebTarget) ClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
        StatService statService = target.proxy(StatService.class);
        return statService.stat(token, month, format);
    }

    public static Response getHealthCheckResponse(String url) {
        log.error("HealthCheck - , url:{} ", url);
        Builder request = ClientBuilder.newClient().target(url).request();
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.error("AuthClientFactory::getHealthCheckResponse() - response:{}", response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.error("AuthClientFactory::getHealthCheckResponse() - entity:{}", entity);
            return response;
        }
        return null;
    }

    public static TokenResponse requestAccessToken(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {
        log.debug("Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", tokenUrl,
                clientId, clientSecret, scope);
        Response response = null;
        try {
            Builder request = ClientBuilder.newClient().target(tokenUrl).request();
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);

            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>(
                    tokenRequest.getParameters());
            request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

            response = request.post(Entity.form(multivaluedHashMap));
            log.debug("Response for Access Token -  response:{}", response);
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

    public static String getIntrospectionEndpoint(String issuer) throws JsonProcessingException {
        log.trace(" AuthClientFactory::getIntrospectionEndpoint() - issuer:{}", issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";

        Builder request = ClientBuilder.newClient().target(configurationEndpoint).request();
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.trace("AuthClientFactory::getIntrospectionEndpoint() - response:{}", response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("AuthClientFactory::getIntrospectionEndpoint() - entity:{}", entity);
            return Jackson.getElement(entity, "introspection_endpoint");
        }
        return null;
    }

    private static IntrospectionService createIntrospectionService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = null;
        try {
            engine = ClientFactoryUtil.createEngine(followRedirects);
            RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
                    .register(engine);
            ResteasyWebTarget target = (ResteasyWebTarget) ClientBuilder.newClient(restClient.getConfiguration())
                    .target(url);
            return target.proxy(IntrospectionService.class);
        } finally {
            if (engine != null) {
                engine.close();
            }
        }
    }

    public static String getJwksUri(String issuer) throws JsonProcessingException {
        log.trace(" AuthClientFactory::getJwksUri() - issuer:{}", issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";

        Builder request = ClientBuilder.newClient().target(configurationEndpoint).request();
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.trace("\n\n AuthClientFactory::getJwksUri() - response:{}", response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("\n\n AuthClientFactory::getJwksUri() - entity:{}", entity);
            return Jackson.getElement(entity, "jwks_uri");
        }
        return null;
    }

    public static JSONWebKeySet getJSONWebKeys(String jwksUri) {
        log.trace(" AuthClientFactory::getJSONWebKeys() - jwksUri:{}", jwksUri);

        Builder request = ClientBuilder.newClient().target(jwksUri).request();
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        log.trace("AuthClientFactory::getJSONWebKeys() - response:{}", response);

        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            log.trace("AuthClientFactory::getJSONWebKeys() - entity:{}", entity);
            JwkResponse jwkResponse = new JwkResponse(200);
            JSONWebKeySet jwks = null;
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(JSON_WEB_KEY_SET)) {
                    jwks = JSONWebKeySet.fromJSONObject(jsonObj);
                    jwkResponse.setJwks(jwks);
                }
                log.trace("AuthClientFactory::getJSONWebKeys() - jwkResponse:{}, jwks:{}", jwkResponse, jwks);
                return jwks;
            }

        }
        return null;
    }

}
