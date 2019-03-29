/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.fido.u2f;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.gluu.oxauth.model.fido.u2f.U2fConfiguration;
import org.gluu.oxauth.model.uma.UmaConstants;

/**
 * The endpoint at which the requester can obtain FIDO U2F metadata configuration
 * 
 * @author Yuriy Movchan Date: 05/27/2015
 *
 */
public interface U2fConfigurationService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public U2fConfiguration getMetadataConfiguration();

}