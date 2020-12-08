/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.client;

import io.jans.as.client.TokenResponse;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.as.client.TokenClient;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.client.Invocation.Builder;


//for testing 
import io.jans.configapi.auth.service.UmaService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;

public class UmaClient {

    @Inject
    @RestClient
    UMATokenService service;
    
    @Inject
    UmaService umaService;

    public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret,
            String... scopeArray) throws Exception {
        return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.PROTECTION, scopeArray);
    }

    public static Token request(final String tokenUrl, final String umaClientId, final String umaClientSecret,
            UmaScopeType scopeType, String... scopeArray) throws Exception {

        String scope = scopeType.getValue();
        if (scopeArray != null && scopeArray.length > 0) {
            for (String s : scopeArray) {
                scope = scope + " " + s;
            }
        }

        TokenResponse tokenResponse = executetPatRequest(tokenUrl, umaClientId, umaClientSecret, scope);

        if (tokenResponse != null) {

            final String patToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
            }
        }
        return null;
    }

    public static TokenResponse executetPatRequest(final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {

        System.out.println("\n\n UmaClient::executetPatRequest() - tokenUrl = " + tokenUrl+" , clientId = "+clientId+" , clientSecret = "+clientSecret+" , scope = "+scope);
        Builder request = ResteasyClientBuilder.newClient().target(tokenUrl).request();
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope(scope);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(tokenUrl).build())
                .register(engine);
        restClient.property("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        restClient.property("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(tokenUrl);

        System.out.println("\n\n UmaClient::executetPatRequest() - request = " + request);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
       // System.out.println("\n\n UmaClient::executetPatRequest() - response = " + response);
       // System.out.println(" UmaClient::executetPatRequest() - response.getStatus()  = " + response.getStatus() );
        //System.out.println("\n\n UmaClient::executetPatRequest() - response.readEntity(String.class) = " + response.readEntity(String.class));
        
        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            System.out.println("\n\n UmaClient::executetPatRequest() - entity = " + entity);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setEntity(entity);
            tokenResponse.injectDataFromJson(entity);
            System.out.println("\n\n UmaClient::executetPatRequest() - tokenResponse_1 = " + tokenResponse);
            System.out.println("\n\n UmaClient::executetPatRequest() - tokenResponse.getAccessToken()_1 = "
                    + tokenResponse.getAccessToken());
           
            //For local testing - Delete later ?????????
            //requestRpt(tokenUrl, "1802.9dcd98ad-fe2c-4fd9-b717-d9436d9f2009","test1234", scope);
            // End
            
            return tokenResponse;
        }
        return null;

    }
    
    public static TokenResponse requestRpt(final String tokenUrl, final String clientId,
            final String clientSecret, final List<String> scopes) {

        System.out.println("\n\n UmaClient::requestRpt() - tokenUrl = " + tokenUrl+" , clientId = "+clientId+" , clientSecret = "+clientSecret+" , scopes = "+scopes);
      
        String scope = null;
        if (scopes != null && scopes.size() > 0) {
            for (String s : scopes) {
                scope = scope + " " + s;
            }
        }
        
        System.out.println("\n\n UmaClient::requestRpt() - scope = "+scope);
        
        Builder request = ResteasyClientBuilder.newClient().target(tokenUrl).request();       
        TokenRequest tokenRequest = new TokenRequest(GrantType.OXAUTH_UMA_TICKET);
        tokenRequest.setScope(scope);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        //tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
         
        
        final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap(tokenRequest.getParameters());
        System.out.println("\n\n UmaClient::requestRpt() - multivaluedHashMap_1 = "+multivaluedHashMap.toString());
        multivaluedHashMap.add("ticket","0735d6a1-4894-4aff-86c2-9b5a6868362d");
        
        System.out.println("\n\n UmaClient::requestRpt() - multivaluedHashMap_2 = "+multivaluedHashMap.toString());
      
        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(tokenUrl).build())
                .register(engine);
        restClient.property("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        restClient.property("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(tokenUrl);

        System.out.println("\n\n UmaClient::requestRpt() - request = " + request);

        Response response = request.post(Entity.form(multivaluedHashMap));
                //.post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
       // System.out.println("\n\n UmaClient::executetPatRequest() - response = " + response);
       // System.out.println(" UmaClient::executetPatRequest() - response.getStatus()  = " + response.getStatus() );
        //System.out.println("\n\n UmaClient::executetPatRequest() - response.readEntity(String.class) = " + response.readEntity(String.class));
        
        if (response.getStatus() == 200) {
            String entity = response.readEntity(String.class);
            System.out.println("\n\n UmaClient::executetPatRequest() - entity = " + entity);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setEntity(entity);
            tokenResponse.injectDataFromJson(entity);
            System.out.println("\n\n UmaClient::executetPatRequest() - tokenResponse_1 = " + tokenResponse);
            System.out.println("\n\n UmaClient::executetPatRequest() - tokenResponse.getAccessToken()_1 = "
                    + tokenResponse.getAccessToken());

            return tokenResponse;
        }
        return null;

    }


    public static RptIntrospectionResponse getRptStatus(UmaMetadata umaMetadata, String authorization,
            String rptToken) {
        System.out.println("\n\n UmaClient::getRptStatus() - final  umaMetadata = " + umaMetadata + " ,authorization = "
               + authorization + " , rptToken = " + rptToken);

        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getIntrospectionEndpoint()).build())
                .property("Content-Type", MediaType.APPLICATION_JSON).register(engine);
        restClient.property("Authorization", "Basic " + authorization);
        restClient.property("Content-Type", MediaType.APPLICATION_JSON);

        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .property("Content-Type", MediaType.APPLICATION_JSON).target(umaMetadata.getIntrospectionEndpoint());
        System.out.println("\n\n\n UmaClient::getRptStatus() - target = " + target+"\n\n");
        
        UmaRptIntrospectionService proxy = target.proxy(UmaRptIntrospectionService.class);
        RptIntrospectionResponse response = proxy.requestRptStatus(authorization, rptToken, "");
        System.out.println("\n\n\n UmaClient::getRptStatus() - response = " + response+"\n\n");
        //return proxy.requestRptStatus(authorization, rptToken, "");
        return response;
    }

}
