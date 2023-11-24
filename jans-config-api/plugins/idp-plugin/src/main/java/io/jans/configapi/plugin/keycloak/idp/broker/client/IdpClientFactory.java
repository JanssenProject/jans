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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IdpClientFactory {

    private static Logger log = LoggerFactory.getLogger(IdpClientFactory.class);
    private static final String CONTENT_TYPE = "Content-Type";

      public static Response requestAccessToken(final String idpServerUrl, final String tokenUrl, final String clientId,
            final String clientSecret, final String scope) {
        log.error("Request for Access Token -  idpServerUrl:{}, tokenUrl:{}, clientId:{}, clientSecret:{}, scope:{} ", idpServerUrl, tokenUrl,
                clientId, clientSecret, scope);
        Response response = null;
        try {
            Builder request = getClientBuilder(tokenUrl);
            request.header("Authorization", "Basic " + clientId+":"+clientSecret);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            final MultivaluedHashMap<String, String> multivaluedHashMap = new MultivaluedHashMap<>();
            multivaluedHashMap.add( "client_id", clientId);
            multivaluedHashMap.add( "client_secret", clientSecret);
            multivaluedHashMap.add( "grant_type", "client_credentials");
            multivaluedHashMap.add( "redirect_uri", idpServerUrl);
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

    public  Response getSpMetadata(String metadataEndpoint) {
        log.error(" SP Metadata - metadataEndpoint:{}", metadataEndpoint);
        Builder metadataClient = getClientBuilder(metadataEndpoint);
        metadataClient.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = metadataClient.get();
        log.error("SpMetadata- response:{}", response);
      
        if (response != null ) {
            log.error("SP metadata response.getStatusInfo():{}, response.getEntity():{}, response.getEntity().getClass():{}",
                    response.getStatusInfo(), response.getEntity(),response.getEntity().getClass());
        }
        
        return response;
    }

   
    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }    

}
