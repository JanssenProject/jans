/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.client;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.jans.notify.model.NotifyMetadata;

/**
 * Metadata endpoint at which the requester can obtain Notify metadata
 * configuration
 * 
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public interface NotifyMetadataClientService {

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public NotifyMetadata getMetadataConfiguration();

}