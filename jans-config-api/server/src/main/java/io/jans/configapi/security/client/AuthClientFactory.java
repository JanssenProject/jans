/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.client;

import static io.jans.as.model.util.Util.escapeLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

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
        log.debug("Introspect Token - url:{}, header:{}, token:{} ,followRedirects:{} ", url, header, token,
                followRedirects);
        RestClientBuilder client = getRestClientBuilder(url);
        ResteasyWebTarget target = (ResteasyWebTarget) ClientBuilder.newClient(client.getConfiguration())
                .property(CONTENT_TYPE, MediaType.APPLICATION_JSON).target(url);
        IntrospectionService proxy = target.proxy(IntrospectionService.class);
        return proxy.introspectToken(header, token);
    }

    public static JsonNode getStatResponse(String url, String token, String month, String format) {
        if (log.isDebugEnabled()) {
            log.debug("Stat Response Token - url:{}, token:{}, month:{} ,format:{} ", escapeLog(url), escapeLog(token),
                    escapeLog(month), escapeLog(format));
        }
        RestClientBuilder restClientBuilder = getRestClientBuilder(url);
        ResteasyWebTarget webTarget = (ResteasyWebTarget) ClientBuilder.newClient(restClientBuilder.getConfiguration())
                .target(url);
        StatService statService = webTarget.proxy(StatService.class);
        return statService.stat(token, month, format);
    }

    public static JsonNode getHealthCheckResponse(String url) {
        log.debug("HealthCheck - , url:{} ", url);
        Builder clientRequest = getClientBuilder(url);
        clientRequest.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response healthResponse = clientRequest.get();
        if (healthResponse.getStatus() == 200) {
            JsonNode jsonNode = healthResponse.readEntity(JsonNode.class);
            log.trace("Health Check Response is - jsonNode:{}", jsonNode);
            return jsonNode;
        }
        return null;
    }

    public static TokenResponse requestAccessToken(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {
        log.debug("Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", tokenUrl,
                clientId, clientSecret, scope);
        Response response = null;
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(scope);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            Builder request = getClientBuilder(tokenUrl);
            request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
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

    public static String getIntrospectionEndpoint(String issuer) throws JsonProcessingException {
        log.debug(" Get Introspection Endpoint - issuer:{}", issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        Builder introspectionClient = getClientBuilder(configurationEndpoint);
        introspectionClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response introspectionResponse = introspectionClient.get();
        log.trace("AuthClientFactory::getIntrospectionEndpoint() - introspectionResponse:{}", introspectionResponse);
        if (introspectionResponse.getStatus() == 200) {
            String introspectionEntity = introspectionResponse.readEntity(String.class);
            log.trace("AuthClientFactory::getIntrospectionEndpoint() - introspectionEntity:{}", introspectionEntity);
            return Jackson.getElement(introspectionEntity, "introspection_endpoint");
        }
        return null;
    }

    private static IntrospectionService createIntrospectionService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = null;
        try {
            engine = ClientFactoryUtil.createEngine(followRedirects);
            RestClientBuilder restClient = getRestClientBuilder(url).register(engine);
            ResteasyWebTarget resteasyWebTarget = (ResteasyWebTarget) ClientBuilder
                    .newClient(restClient.getConfiguration()).target(url);
            return resteasyWebTarget.proxy(IntrospectionService.class);
        } finally {
            if (engine != null) {
                engine.close();
            }
        }
    }

    public static String getJwksUri(String issuer) throws JsonProcessingException {
        log.trace(" Jwks Uri - issuer:{}", issuer);
        String configurationEndpoint = issuer + "/.well-known/openid-configuration";
        Builder jwksUriClient = getClientBuilder(configurationEndpoint);
        jwksUriClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response jwksUriResponse = jwksUriClient.get();
        log.trace("AuthClientFactory::getJwksUri() - jwksUriResponse:{}", jwksUriResponse);
        if (jwksUriResponse.getStatus() == 200) {
            String jwksEntity = jwksUriResponse.readEntity(String.class);
            log.trace("AuthClientFactory::getJwksUri() - jwksEntity:{}", jwksEntity);
            return Jackson.getElement(jwksEntity, "jwks_uri");
        }
        return null;
    }

    public static JSONWebKeySet getJSONWebKeys(String jwksUri) {
        log.debug("JSONWebKeys - jwksUri:{}", jwksUri);
        Builder clientBuilder = getClientBuilder(jwksUri);
        clientBuilder.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response webKeyResponse = clientBuilder.get();
        log.trace("AuthClientFactory::getJSONWebKeys() - webKeyResponse:{}", webKeyResponse);
        if (webKeyResponse.getStatus() == 200) {
            String jsonWebKeySetEntity = webKeyResponse.readEntity(String.class);
            log.trace("AuthClientFactory::getJSONWebKeys() - jsonWebKeySetEntity:{}", jsonWebKeySetEntity);
            JwkResponse jwkResponse = new JwkResponse(200);
            JSONWebKeySet jwks = null;
            if (StringUtils.isNotBlank(jsonWebKeySetEntity)) {
                JSONObject jsonObj = new JSONObject(jsonWebKeySetEntity);
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

    private static RestClientBuilder getRestClientBuilder(String clientUrl) {
        return RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(clientUrl).build());
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
