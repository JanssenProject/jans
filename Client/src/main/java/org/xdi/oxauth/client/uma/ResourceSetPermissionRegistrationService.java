package org.xdi.oxauth.client.uma;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.UmaConstants;

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
public interface ResourceSetPermissionRegistrationService {
	@PUT
	@Path("{host}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetPermissionTicket registerResourceSetPermission(@HeaderParam("Authorization") String authorization, @HeaderParam("Host") String amHost,
			@PathParam("host") String host, ResourceSetPermissionRequest resourceSetPermissionRequest);
}