package org.xdi.oxauth.uma.ws.rs;

import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * This AM's endpoint is part of resource set registration API.
 * <p/>
 * The host uses the RESTful API at the AM's resource set registration endpoint
 * to create, read, update, and delete resource set descriptions, along with
 * listing groups of such descriptions. The host MUST use its valid PAT obtained
 * previously to gain access to this endpoint. The resource set registration API
 * is a subset of the protection API.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *
 * Date: 10/03/2012
 */
@Path("/host/rsrc/resource_set")
public interface ResourceSetRegistrationRestWebService {
	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response putResourceSet(@HeaderParam("Authorization") String authorization, @HeaderParam("If-Match") String rsver,
			@PathParam("rsid") String rsid, ResourceSet resourceSet);

	@GET
	@Path("{rsid}")
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response getResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

    /**
     * Gets resource set lists.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param p_scope       scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public List<String> getResourceSetList(@HeaderParam("Authorization") String authorization, @QueryParam("scope") String p_scope);

	@DELETE
	@Path("{rsid}")
	public Response deleteResourceSet(@HeaderParam("Authorization") String authorization, @HeaderParam("If-Match") String rsver,
			@PathParam("rsid") String rsid);

}