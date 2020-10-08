/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package org.gluu.oxauth.ws.rs.fido.u2f;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.fido.u2f.U2fConfiguration;
import org.gluu.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.gluu.oxauth.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
	@Produces({ "application/json" })
	public Response getConfiguration() {
		try {
		    if (appConfiguration.getDisableU2fEndpoint()) {
	            return Response.status(Status.FORBIDDEN).build();
		    }

		    final String baseEndpointUri = appConfiguration.getBaseEndpoint();

			final U2fConfiguration conf = new U2fConfiguration();
			conf.setVersion("2.0");
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
					.entity(errorResponseFactory.errorAsJson(U2fErrorResponseType.SERVER_ERROR, "Unknown.")).build());
		}
	}

}
