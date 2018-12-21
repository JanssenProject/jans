/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.fido2.ws.rs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.gluu.oxauth.fido2.service.DataMapperService;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.fido.u2f.U2fConfiguration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * The endpoint at which the requester can obtain FIDO2 metadata
 * configuration
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@ApplicationScoped
@Path("/fido2/configuration")
@Api(value = "/.well-known/fido2-configuration", description = "The FIDO2 server endpoint that provides configuration data in a JSON [RFC4627] document that resides in at /.well-known/fido-configuration directory at its hostmeta [hostmeta] location. The configuration data documents conformance options and endpoints supported by the FIDO2 server.")
public class ConfigurationController {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

	@GET
	@Produces({ "application/json" })
	@ApiOperation(value = "Provides configuration data as json document. It contains options and endpoints supported by the FIDO U2F server.", response = U2fConfiguration.class)
	public Response getConfiguration() {
        if ((appConfiguration.getFido2Configuration() == null) || appConfiguration.getFido2Configuration().isDisable()) {
            return Response.status(Status.FORBIDDEN).build();
        }

	    final String baseEndpointUri = appConfiguration.getBaseEndpoint();
	    ObjectNode response = dataMapperService.createObjectNode();
        
        response.put("version", "1.0");
        response.put("issuer", appConfiguration.getIssuer());

        ObjectNode attestation = dataMapperService.createObjectNode();
        response.set("attestation", attestation);
        attestation.put("base_path", baseEndpointUri + "/fido2/attestation");
        attestation.put("options_enpoint", baseEndpointUri + "/fido2/attestation/options");
        attestation.put("result_enpoint", baseEndpointUri + "/fido2/attestation/result");

        ObjectNode assertion = dataMapperService.createObjectNode();
        response.set("assertion", assertion);
        assertion.put("base_path", baseEndpointUri + "/fido2/assertion");
        assertion.put("options_enpoint", baseEndpointUri + "/fido2/assertion/options");
        assertion.put("result_enpoint", baseEndpointUri + "/fido2/assertion/result");

        ResponseBuilder builder = Response.ok().entity(response.toString());
        return builder.build();
	}

}
