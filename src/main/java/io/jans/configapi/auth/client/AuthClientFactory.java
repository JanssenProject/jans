/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.client;

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


import javax.ws.rs.core.MediaType;

public class AuthClientFactory {

    public static OpenIdClientService getIntrospectionService(String url, boolean followRedirects) {
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

    private static OpenIdClientService createIntrospectionService(String url, boolean followRedirects) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(followRedirects);
        RestClientBuilder restClient = RestClientBuilder.newBuilder().baseUri(UriBuilder.fromPath(url).build())
               .register(engine);
        //restClient.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        restClient.property("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
         ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .target(url);
         target.property("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
         OpenIdClientService proxy = target.proxy(OpenIdClientService.class);
        return proxy;
        
        /*
        
        OpenIdClientService proxy = RestClientBuilder.newBuilder()
        		.register(engine).baseUri(UriBuilder.fromPath(url).build()).build(OpenIdClientService.class);
        		
        return proxy;
        */
        /*  ResteasyClient client = ResteasyClientBuilder.newClient().register(OpenIdClientService.class);
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(p_url));
        IntrospectionService proxy = target.proxy(IntrospectionService.class);*/
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
