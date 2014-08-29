package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 * 
 * @author Yuriy Movchan Date: 10/25/2012
 */
@Path("/oxauth/uma-configuration")
@Api(value="/oxauth/uma-configuration", description = "The authorization server MUST provide configuration data in a JSON [RFC4627] document that resides in an /uma-configuration directory at its hostmeta [hostmeta] location. The configuration data documents conformance options and endpoints supported by the authorization server. ")
public interface MetaDataConfigurationRestWebService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response getMetadataConfiguration();

}