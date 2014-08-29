package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the host registers permissions that it anticipates a
 * requester will shortly be asking for from the AM. This AM's endpoint is part
 * of resource set registration API.
 * <p/>
 * In response to receiving an access request accompanied by an RPT that is
 * invalid or has insufficient authorization data, the host SHOULD register a
 * permission with the AM that would be sufficient for the type of access
 * sought. The AM returns a permission ticket for the host to give to the
 * requester in its response.
 * 
 * @author Yuriy Movchan Date: 10/11/2012
 */
@Path("/host/rsrc_pr")
@Api(value="/host/rsrc_pr", description = "The endpoint at which the host registers permissions that it anticipates a " +
        " requester will shortly be asking for from the AM. This AM's endpoint is part " +
        " of resource set registration API.")
public interface ResourceSetPermissionRegistrationRestWebService {
	@PUT
	@Path("{host}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response registerResourceSetPermission(@Context HttpServletRequest request, @HeaderParam("Authorization") String authorization, @HeaderParam("Host") String amHost,
			@PathParam("host") String host, ResourceSetPermissionRequest resourceSetPermissionRequest);

    // yuriyz 02.04.2013: commented this endpoint, it's not present in latest specs
//	@GET
//	@Path("{host}/{configurationCode}")
//	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
//	public Response getResourceSetPermission(@Context HttpServletRequest request, @HeaderParam("Authorization") String authorization, @HeaderParam("Host") String amHost,
//			@PathParam("host") String host, @PathParam("configurationCode") String configurationCode);

}