/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller.u2f;

import io.jans.as.model.config.Constants;
import io.jans.fido2.model.conf.AppConfiguration;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.u2f.U2fConfiguration;
import io.jans.fido2.model.u2f.U2fErrorResponseType;
import io.jans.fido2.model.u2f.util.ServerUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;


/**
 * The endpoint at which the requester can obtain FIDO U2F metadata
 * configuration
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
@ApplicationScoped
@Path("/fido-configuration")
public class U2fConfigurationWS {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @GET
    @Produces({"application/json"})
    public Response getConfiguration() {
        try {

            final String baseEndpointUri = appConfiguration.getBaseEndpoint();

            final U2fConfiguration conf = new U2fConfiguration();
            conf.setVersion("2.1");
            conf.setIssuer(appConfiguration.getIssuer());

            conf.setRegistrationEndpoint(baseEndpointUri + "/fido/u2f/registration");
            conf.setAuthenticationEndpoint(baseEndpointUri + "/fido/u2f/authentication");

            // convert manually to avoid possible conflicts between resteasy
            // providers, e.g. jettison, jackson
            final String entity = ServerUtil.asPrettyJson(conf);
            log.trace("FIDO U2F configuration: {}", entity);

            return Response.ok(entity).build();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.errorAsJson(U2fErrorResponseType.SERVER_ERROR, Constants.UNKNOWN)).build());
        }
    }

}
