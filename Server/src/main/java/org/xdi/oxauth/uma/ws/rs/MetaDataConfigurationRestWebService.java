/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 * 
 * @author Yuriy Movchan Date: 10/25/2012
 */
@Path("/oxauth/uma-configuration")
@Api(value="/oxauth/uma-configuration", description = "The authorization server endpiont that provides configuration data in a JSON [RFC4627] document that resides in an /uma-configuration directory at its hostmeta [hostmeta] location. The configuration data documents conformance options and endpoints supported by the authorization server. ")
public interface MetaDataConfigurationRestWebService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
    @ApiOperation(
         value = "Provides configuration data as json document. It contains options and endpoints supported by the authorization server.",
         response = UmaConfiguration.class
    )
	public Response getMetadataConfiguration();

}