/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.client;

import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;


public class UmaClient {

    @Inject
    @RestClient
    UMATokenService service;

    public static RptIntrospectionResponse getRptStatus(UmaMetadata umaMetadata, String authorization,
            String rptToken) {
        ApacheHttpClient43Engine engine = ClientFactory.createEngine(false);
        RestClientBuilder restClient = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromPath(umaMetadata.getIntrospectionEndpoint()).build())
                .property("Content-Type", MediaType.APPLICATION_JSON).register(engine);
        restClient.property("Authorization", "Basic " + authorization);
        restClient.property("Content-Type", MediaType.APPLICATION_JSON);

        ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient(restClient.getConfiguration())
                .property("Content-Type", MediaType.APPLICATION_JSON).target(umaMetadata.getIntrospectionEndpoint());

        UmaRptIntrospectionService proxy = target.proxy(UmaRptIntrospectionService.class);
        RptIntrospectionResponse response = proxy.requestRptStatus(authorization, rptToken, "");

        return response;
    }

}
