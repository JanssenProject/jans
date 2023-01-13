/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import io.jans.fido2.model.conf.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import io.jans.fido2.service.DataMapperService;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The endpoint at which the requester can obtain FIDO2 metadata
 * configuration
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@ApplicationScoped
@Path("/configuration")
public class ConfigurationController {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

    @GET
    @Produces({"application/json"})
    public Response getConfiguration() {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        final String baseEndpointUri = appConfiguration.getBaseEndpoint();
        ObjectNode response = dataMapperService.createObjectNode();

        response.put("version", "1.1");
        response.put("issuer", appConfiguration.getIssuer());

        ObjectNode attestation = dataMapperService.createObjectNode();
        response.set("attestation", attestation);
        attestation.put("base_path", baseEndpointUri + "/attestation");
        attestation.put("options_enpoint", baseEndpointUri + "/attestation/options");
        attestation.put("result_enpoint", baseEndpointUri + "/attestation/result");

        ObjectNode assertion = dataMapperService.createObjectNode();
        response.set("assertion", assertion);
        assertion.put("base_path", baseEndpointUri + "/assertion");
        assertion.put("options_enpoint", baseEndpointUri + "/assertion/options");
        assertion.put("result_enpoint", baseEndpointUri + "/assertion/result");

        if (appConfiguration.isUseSuperGluu()) {
            response.put("super_gluu_registration_endpoint", baseEndpointUri + "/registration");
            response.put("super_gluu_authentication_endpoint", baseEndpointUri + "/authentication");
        }

        ResponseBuilder builder = Response.ok().entity(response.toString());
        return builder.build();
    }

}
