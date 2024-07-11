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
import jakarta.ws.rs.core.Response.Status;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.apache.http.HttpEntity;

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
        log.error("Request for Access Token -  tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", tokenUrl,
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
        String tokenUrl = this.appConfiguration.getTokenUrl(); 
        String clientId =  this.appConfiguration.getClientId();

        String clientSecret = this.appConfiguration.getClientPassword();
        String scopes = "https://jans.io/oauth/lock/telemetry.write";
        String accessToken = null;
        Integer expiresIn = 0;
        TokenResponse tokenResponse = requestAccessToken(tokenUrl, clientId, clientSecret, scopes);
                if (tokenResponse != null) {

                    log.error("Token Response - tokenScope: {}, tokenAccessToken: {} ", tokenResponse.getScope(),
                            tokenResponse.getAccessToken());
                    accessToken = tokenResponse.getAccessToken();
                    expiresIn = tokenResponse.getExpiresIn();
                   
                }
        log.error("getToken accessToken:{}", accessToken, expiresIn);
        
        return accessToken;
    }
    
    public HttpServiceResponse postData(String postData) {
        log.error("NEw postData postData:{}", postData);
        
        //String uri = "jans-config-api/lock/audit/telemetry";
        String uri = "https://pujavs-probable-alpaca.gluu.info/jans-config-api/lock/audit/telemetry";
       
        String authData = getToken();
        String authType= "Bearer ";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        ContentType contentType = ContentType.APPLICATION_JSON;
        
        HttpServiceResponse response = httpService.executePost(uri, authData, headers, postData, contentType,  authType);

        log.error("response:{}", response);
        return response;
    }
    
    public String getResponseEntityString(HttpServiceResponse serviceResponse) {
        String jsonString = null;
        
        if(serviceResponse == null) {
            return jsonString;
        }
        
        if (serviceResponse != null && serviceResponse.getHttpResponse()!=null && serviceResponse.getHttpResponse().getStatusLine()!=null && serviceResponse.getHttpResponse().getStatusLine().getStatusCode() == Status.OK.getStatusCode()) {
            HttpEntity entity = serviceResponse.getHttpResponse().getEntity();
            if (entity==null) {
                return jsonString;
            }
            jsonString = entity.toString();

        }
        return jsonString;
    }
    
    
    public void getAppConfiguration() {
        log.error("appConfiguration:{}", appConfiguration);
        log.error("appConfiguration.getClientId():{}", appConfiguration.getClientId());
        log.error("appConfiguration.getClientPassword():{}", appConfiguration.getClientPassword());
        log.error("appConfiguration.getTokenUrl():{}", appConfiguration.getTokenUrl());
        log.error(" appConfiguration.getEndpointDetails():{}", appConfiguration.getEndpointDetails());
    }
    
    
    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

   
    
    
    

}
