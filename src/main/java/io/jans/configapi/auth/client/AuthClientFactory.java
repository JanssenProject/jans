/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.client;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.configapi.auth.client.OpenIdClientService;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

public class AuthClientFactory {

    public static IntrospectionService getIntrospectionService(String url,boolean followRedirects) {
        return createIntrospectionService(url, followRedirects);
    }

    public static UmaMetadataService getUmaMetadataService(String umaMetadataUri, boolean followRedirects) {
        return createUmaMetadataService(umaMetadataUri, followRedirects);
    }

    public static UmaPermissionService getUmaPermissionService(UmaMetadata umaMetadata, boolean followRedirects) {
        return createUmaPermissionService(umaMetadata);
    }

    public static UmaRptIntrospectionService getUmaRptIntrospectionService(UmaMetadata umaMetadata,
            boolean followRedirects) {
        return createUmaRptIntrospectionService(umaMetadata);
    }
    
    public static IntrospectionResponse getIntrospectionResponse(String url,String header, String token,boolean followRedirects) {
        System.out.println("\n\n\n AuthClientFactory::getIntrospectionResponse() - url = "+url+" ,header="+header+" ,token = "+token);
       /* ApacheHttpClient43Engine engine = ClientFactory.createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
               .register(engine);
         restClient.property("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
         ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
           Form form = new Form();
         form.param("token", token);
         Entity<Form> entity = Entity.form(form);
         Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED).post(entity);
         System.out.println("\n\n\n AuthClientFactory::getIntrospectionResponse() - response = "+response);
         //Response response = target.request(MediaType.APPLICATION_JSON).post(Entity.form(new MultivaluedHashMap<String, String>(token)));
         String value = response.readEntity(String.class);
         System.out.println(value);
         response.close();
     */
        Client client = ResteasyClientBuilder.newClient();
        WebTarget target = client.target(url);
        Form form = new Form();
        form.param("token", token);
        Entity<Form> entity = Entity.form(form);
        Response response = target.request(MediaType.APPLICATION_JSON).header("Authorization", header).post(entity);
        String value = response.readEntity(String.class);
        System.out.println(value);
        response.close();  
        
       return new IntrospectionResponse();
    }

    private static IntrospectionService createIntrospectionService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
               .register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
         IntrospectionService proxy = target.proxy(IntrospectionService.class);
        return proxy;
        

    }

    private static UmaMetadataService createUmaMetadataService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
                .register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
        UmaMetadataService proxy = target.proxy(UmaMetadataService.class);
        return proxy;
    }

    private static UmaPermissionService createUmaPermissionService(UmaMetadata umaMetadata) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getPermissionEndpoint()).build()).register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(umaMetadata.getPermissionEndpoint());
        UmaPermissionService proxy = target.proxy(UmaPermissionService.class);
        return proxy;
    }

    private static UmaRptIntrospectionService createUmaRptIntrospectionService(UmaMetadata umaMetadata) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getIntrospectionEndpoint()).build()).register(engine);
        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(umaMetadata.getPermissionEndpoint());
        UmaRptIntrospectionService proxy = target.proxy(UmaRptIntrospectionService.class);
        return proxy;
    }
    
  
    
}
