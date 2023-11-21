/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.client;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.util.Jackson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;
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

@ApplicationScoped
public class IdpClientFactory {

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String KEYCLOAK_URL = "http://localhost:8180/";
    private static final String KEYCLOAK_REALM = "master";
    private static final String KEYCLOAK_CLIENT_ID = "my-client-1";
    private static final String KEYCLOAK_CLIENT_SECRET = "aqOMI7DhNxCFbW0IieBHSrdA6HMTwxiQ";
    private static final String USER_ID = "admin1";
    private static final String KEYCLOAK_USER_PWD = "admin123"; 
    private static final String KEYCLOAK_TOKEN_URL = "http://localhost:8180/realms/master/protocol/openid-connect/token";
    private static final String KEYCLOAK_IMPORT_CONFIG_URL = "http://localhost:8180/admin/realms/keycloak-internal-identity/identity-provider/import-config";

    private static Logger log = LoggerFactory.getLogger(IdpClientFactory.class);

    public static JsonNode getHealthCheckResponse(String url) {
        log.error("HealthCheck - , url:{} ", url);
        Builder clientRequest = getClientBuilder(url);
        clientRequest.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response healthResponse = clientRequest.get();
        if (healthResponse.getStatus() == 200) {
            JsonNode jsonNode = healthResponse.readEntity(JsonNode.class);
            log.error("Health Check Response is - jsonNode:{}", jsonNode);
            return jsonNode;
        }
        return null;
    }

    public static Response requestAccessToken(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {
        log.error("Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", tokenUrl,
                clientId, clientSecret, scope);
        Response response = null;
        try {
            Builder request = getClientBuilder(KEYCLOAK_TOKEN_URL);
            request.header("Authorization", "Basic " + "abc");
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
            multivaluedHashMap.add( "client_id", clientId);
            multivaluedHashMap.add( "client_secret", clientSecret);
            //multivaluedHashMap.add( "grant_type", GrantType.CLIENT_CREDENTIALS);
            multivaluedHashMap.add( "grant_type", "client_credentials");
            multivaluedHashMap.add( "redirect_uri", KEYCLOAK_URL);
            log.error("Request for Access Token -  multivaluedHashMap:{}", multivaluedHashMap);
            
            response = request.post(Entity.form(multivaluedHashMap));
            log.error("Response for Access Token -  response:{}", response);
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                log.error("Access Token -  entity:{}", entity);
                return response;
            }
        } catch(Exception ex){
            ex.printStackTrace();
            log.error("IdpClientFactory Exception  requestAccessToken is :{}", ex);
        }finally {
        

            if (response != null) {
                response.close();
            }
        }
        return response;
    }

    public static Response getClients(String issuer) throws JsonProcessingException {
        log.error(" Jwks Uri - issuer:{}", issuer);
        String configurationEndpoint = "http://localhost:8180";
        Builder jwksUriClient = getClientBuilder(configurationEndpoint);
        jwksUriClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = jwksUriClient.get();
        log.error("AuthClientFactory::getJwksUri() - response:{}", response);
      
        return response;
    }

    public static Response getJSONWebKeys(String jwksUri) {
        log.error("JSONWebKeys - jwksUri:{}", jwksUri);
        Builder clientBuilder = getClientBuilder(jwksUri);
        clientBuilder.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = clientBuilder.get();
        log.error("AuthClientFactory::getJSONWebKeys() - response:{}", response);
        
        return response;
    }

    /*
     * public static Response revokeSession(String url, String token, String userId)
     * { log.error("Request for Access Token -  url:{}, token:{}, userId:{} ", url,
     * token, userId); Response response = null; try {
     * 
     * Builder request = getClientBuilder(url); request.header("Authorization",
     * "Basic " + token); request.header(CONTENT_TYPE,
     * MediaType.APPLICATION_FORM_URLENCODED); final MultivaluedHashMap<String,
     * String> multivaluedHashMap = new MultivaluedHashMap<>(
     * "revokeSessionRequest.getParameters()"); response =
     * request.post(Entity.form(multivaluedHashMap));
     * log.error("Response for Access Token -  response:{}", response);
     * 
     * } finally {
     * 
     * if (response != null) { response.close(); } } return response; }
     */

    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

}
