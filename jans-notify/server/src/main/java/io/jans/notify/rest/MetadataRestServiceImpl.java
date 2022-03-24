/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import io.jans.notify.model.NotifyMetadata;
import io.jans.notify.model.conf.Configuration;
import org.slf4j.Logger;

/**
 * The endpoint at which the requester can obtain oxNotify metadata configuration
 *
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
@Path("/notify-configuration")
public class MetadataRestServiceImpl implements MetadataRestService {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	public Response getConfiguration() {
		try {
			String baseEndpointUri = configuration.getBaseEndpoint();
			String issuer = configuration.getIssuer();

			NotifyMetadata notifyMetadata = new NotifyMetadata();
			notifyMetadata.setVersion("1.0");
			notifyMetadata.setIssuer(issuer);

			notifyMetadata.setNotifyEndpoint(baseEndpointUri + "/restv1");
			
			log.trace("Notify metadata: {0}", notifyMetadata);

			return Response.ok(notifyMetadata).build();
		} catch (Throwable ex) {
			log.error(ex.getMessage(), ex);
			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
					"The notify server encountered an unexpected condition which prevented it from fulfilling the request.")
					.build());
		}
	}

}
