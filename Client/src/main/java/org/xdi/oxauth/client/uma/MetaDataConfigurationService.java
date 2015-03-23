/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 * 
 * @author Yuriy Movchan Date: 10/25/2012
 */
public interface MetaDataConfigurationService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public UmaConfiguration getMetadataConfiguration();

}