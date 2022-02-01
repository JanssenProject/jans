/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs.fido.u2f;

import io.jans.as.model.common.ComponentType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.fido.u2f.U2fConfiguration;
import io.jans.as.model.fido.u2f.U2fErrorResponseType;
import io.jans.as.server.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the requester can obtain FIDO U2F metadata
 * configuration
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
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
            errorResponseFactory.validateComponentEnabled(ComponentType.U2F);

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
