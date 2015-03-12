/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.*;
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
@Api(value="/host/rsrc/resource_set", description = "Resource set registration endpoint to create, read, update, and delete resource set descriptions, along with retrieving lists of such descriptions.")
public interface ResourceSetRegistrationRestWebService {

    @POST
  	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
   	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
   	public Response createResourceSet(@HeaderParam("Authorization") String authorization, ResourceSet resourceSet);

	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public Response updateResourceSet(@HeaderParam("Authorization") String authorization,
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
	public Response deleteResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

    @HEAD
    public Response unsupportedHeadMethod();

    @OPTIONS
    public Response unsupportedOptionsMethod();

}