package io.jans.lock.service.util;


import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.net.HttpService;
import io.jans.model.net.HttpServiceResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class AuthUtil {
    
    private static final String CONTENT_TYPE = "Content-Type";

    
    @Inject
    Logger log;
    
    @Inject
    AppConfiguration appConfiguration;

    @Inject 
    HttpService httpService;
    
    
    public TokenResponse requestAccessToken(final String tokenUrl, final String clientId,
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
    
    public String getToken() {
        log.error("\n\n Request for token \n\n");
        String tokenUrl = "https://pujavs-probable-alpaca.gluu.info/jans-auth/restv1/token"; 
        String clientId = "1800.59d18a30-51df-4f96-a83f-f31d413e9e5b";

        String clientSecret = "nfpc90t6ByZK";
        String scopes = "https://jans.io/oauth/lock/telemetry.write";
        String accessToken = null;
        Integer expiresIn = 0;
        TokenResponse tokenResponse = requestAccessToken(tokenUrl, clientId, clientSecret, scopes);
                if (tokenResponse != null) {

                    log.debug("Token Response - tokenScope: {}, tokenAccessToken: {} ", tokenResponse.getScope(),
                            tokenResponse.getAccessToken());
                    accessToken = tokenResponse.getAccessToken();
                    expiresIn = tokenResponse.getExpiresIn();
                   
                }
        log.error("getToken accessToken:{}", accessToken, expiresIn);
        
        return accessToken;
    }
    
    public void postData(String postData) {
        log.error("postData postData:{}", postData);
        
        String uri = "jans-config-api/lock/audit/telemetry";
        //String authData="459477ac-e162-499b-b918-847384aadfb9";
        String authData = getToken();
        String authType= "Bearer ";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        ContentType contentType = ContentType.APPLICATION_JSON;
        
        HttpServiceResponse response = httpService.executePost(uri, authData, headers, postData, contentType,  authType);

        log.error("response:{}", response);
        
    }
    
    
    public void getAppConfiguration() {
        log.error("appConfiguration:{}", appConfiguration);
        log.error("appConfiguration.getApiClientId():{}", appConfiguration.getApiClientId());
        log.error("appConfiguration.getApiClientPassword():{}", appConfiguration.getApiClientPassword());
        log.error("appConfiguration.getTokenUrl():{}", appConfiguration.getTokenUrl());
        log.error(" appConfiguration.getEndpointDetails():{}", appConfiguration.getEndpointDetails());
    }
    
    
    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

    
    
    

}
