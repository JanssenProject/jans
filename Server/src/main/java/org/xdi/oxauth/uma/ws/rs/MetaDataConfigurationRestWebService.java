package org.xdi.oxauth.uma.ws.rs;

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
public interface MetaDataConfigurationRestWebService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response getMetadataConfiguration();

}