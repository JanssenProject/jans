/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.client;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.client.ssa.get.*;
import io.jans.as.client.ssa.create.SsaCreateClient;
import io.jans.as.client.service.StatService;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.RevokeSessionResponse;
import io.jans.as.client.RevokeSessionRequest;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwk.JSONWebKeySet;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

import io.jans.configapi.core.util.Jackson;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;


import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RegisterProvider(OpenIdClientService.class)
@RegisterProvider(StatClient.class)
@ApplicationScoped
public class AuthClientFactory {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String OPENID_CONFIGURATION_URL = "/.well-known/openid-configuration";
    private static Logger log = LoggerFactory.getLogger(AuthClientFactory.class);

    public static IntrospectionService getIntrospectionService(String url, boolean followRedirects) {
        return createIntrospectionService(url, followRedirects);
    }

    public static IntrospectionResponse getIntrospectionResponse(String url, String header, String token,
            boolean followRedirects) {
        log.debug("Introspect Token - url:{}, header:{}, token:{} ,followRedirects:{} ", url, header, token,
                followRedirects);
        ResteasyWebTarget target = (ResteasyWebTarget) ClientBuilder.newClient()
                .property(CONTENT_TYPE, MediaType.APPLICATION_JSON).target(url);
        IntrospectionService proxy = target.proxy(IntrospectionService.class);
        return proxy.introspectToken(header, token);
    }

    public static JsonNode getStatResponse(String url, String token, String month, String startMonth, String endMonth,
            String format) {
        if (log.isDebugEnabled()) {
            log.debug("Stat Response Token - url:{}, token:{}, month:{}, startMonth:{}, endMonth:{}, format:{} ",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                    escapeLog(format));
        }
        ResteasyWebTarget webTarget = (ResteasyWebTarget) ClientBuilder.newClient().target(url);
        StatService statService = webTarget.proxy(StatService.class);
        return statService.stat(token, month, startMonth, endMonth, format);
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

    public static Response revokeToken(final String revokeUrl, final String clientId, final String token,
            final String tokenTypeHint) {
        log.debug("Request for Access Token -  revokeUrl:{}, clientId:{}, token:{} , tokenTypeHint:{}", revokeUrl,
                clientId, token, tokenTypeHint);

        Builder request = getClientBuilder(revokeUrl);
        request.header(AUTHORIZATION, token);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        log.debug(" request:{}}", request);
        MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
        multivaluedHashMap.add("token", token);
        multivaluedHashMap.add("token_type_hint", tokenTypeHint);
        multivaluedHashMap.add("client_id", clientId);

        Response response = request.post(Entity.entity(Entity.form(multivaluedHashMap), MediaType.APPLICATION_JSON));
        log.debug(" response:{}", response);

        return response;
    }

    public static String getIntrospectionEndpoint(String issuer) throws JsonProcessingException {
        log.debug(" Get Introspection Endpoint - issuer:{}", issuer);
        String configurationEndpoint = issuer + OPENID_CONFIGURATION_URL;
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
            ResteasyWebTarget resteasyWebTarget = (ResteasyWebTarget) ClientBuilder.newClient().target(url);
            return resteasyWebTarget.proxy(IntrospectionService.class);
        } finally {
            if (engine != null) {
                engine.close();
            }
        }
    }

    public static SsaGetResponse getSsaList(final String issuer, final String accessToken, final String jti,
            final String orgId) throws Exception {
        log.error("Request jti SSA List -  issuer:{}, accessToken:{}, jti:{} , orgId:{}", issuer, accessToken, jti,
                orgId);
        String ssaEndpoint = getSsaEndpoint(issuer);
        ResteasyWebTarget webTarget = (ResteasyWebTarget) ClientBuilder.newClient().target(ssaEndpoint);
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, jti, orgId);

        return ssaGetResponse;
    }

    public static Response getSsa(final String issuer, final String accessToken, final String jti, final String orgId)
            throws Exception {
        log.error("NEW Request jti SSA List -  issuer:{}, accessToken:{}, jti:{} , orgId:{}", issuer, accessToken, jti,
                orgId);

        String ssaEndpoint = getSsaEndpoint(issuer);
        log.error("Get SSA -  ssaEndpoint:{}", ssaEndpoint);
        
        Builder request = getClientBuilder(ssaEndpoint);
        request.header(AUTHORIZATION, accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.buildGet().property("jti", jti).property("org_id", orgId);
        request.accept(MediaType.APPLICATION_JSON);
        
        log.error("Get SSA request:{}}", request);
       
        Response response = request.get();
        log.error("GET SSA response:{}", response);

        return response;
    }

    public static Response revokeSsa(final String issuer, final String accessToken, final String jti,
            final String orgId) throws Exception {
        log.error("Revoke jti -  issuer:{}, accessToken:{}, jti:{} , orgId:{}", issuer, accessToken, jti, orgId);

        String ssaEndpoint = getSsaEndpoint(issuer);
        log.error("Revoke SSA -  ssaEndpoint:{}", ssaEndpoint);
        
        
        Builder request = getClientBuilder(ssaEndpoint);
        request.header(AUTHORIZATION, accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.buildDelete().property("jti", jti).property("org_id", orgId);
        log.error("Revoke request:{}}", request);

        Response response = request.delete();
        log.error(" Revoke response:{}", response);

        return response;
    }

    public static Response createSsa(final String issuer, final String accessToken, final String jsonNode)
            throws Exception {
        log.error("NEW Create SSA -  issuer:{}, accessToken:{}, jsonNode:{}", issuer, accessToken, jsonNode);

        String ssaEndpoint = getSsaEndpoint(issuer);
        log.error("Create SSA -  ssaEndpoint:{}", ssaEndpoint);
        
        Builder request = getClientBuilder(ssaEndpoint);
        log.error("Create SSA -  request:{}", request);
        request.header(AUTHORIZATION, accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        log.error(" request:{}", request);
        
        MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
        multivaluedHashMap.add("accessToken", accessToken);
        multivaluedHashMap.add("requestParams", jsonNode);

        Response response = request.post(Entity.entity(Entity.form(multivaluedHashMap), MediaType.APPLICATION_JSON));

        return response;
    }

    public static String getJwksUri(String issuer) throws JsonProcessingException {
        log.trace(" Jwks Uri - issuer:{}", issuer);
        String configurationEndpoint = issuer + OPENID_CONFIGURATION_URL;
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

    public static RevokeSessionResponse revokeSession(String url, String token, String userId) {
        log.debug("Request for Access Token -  url:{}, token:{}, userId:{} ", url, token, userId);
        Response response = null;
        try {
            RevokeSessionRequest revokeSessionRequest = new RevokeSessionRequest("uid", "test");

            Builder request = getClientBuilder(url);
            request.header(AUTHORIZATION, "Basic " + token);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>(
                    revokeSessionRequest.getParameters());
            response = request.post(Entity.form(multivaluedHashMap));
            log.trace("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                RevokeSessionResponse revokeSessionResponse = new RevokeSessionResponse();
                revokeSessionResponse.setEntity(entity);
                revokeSessionResponse.injectDataFromJson(entity);
                return revokeSessionResponse;
            }
        } finally {

            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static String getSsaEndpoint(String issuer) throws JsonProcessingException {
        log.debug(" Get SSA Endpoint - issuer:{}", issuer);

        String configurationEndpoint = issuer + OPENID_CONFIGURATION_URL;
        Builder openIdClient = getClientBuilder(configurationEndpoint);
        openIdClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response openIdResponse = openIdClient.get();
        String endpoint = null;

        if (openIdResponse!=null && openIdResponse.getStatus() == 200) {
            String introspectionEntity = openIdResponse.readEntity(String.class);
            log.trace("introspectionEntity:{}", introspectionEntity);
            endpoint =  Jackson.getElement(introspectionEntity, "ssa_endpoint");
            log.debug("endpoint:{}", endpoint);
            if(StringUtils.isBlank(endpoint)) {
                throw new WebApplicationException("configurationEndpoint {"+configurationEndpoint+"} does not ssa_endpoint is not avialable");
            }
        }
        return endpoint;
    }

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
