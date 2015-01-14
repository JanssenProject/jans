/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetStatus;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.VersionedResourceSet;

/**
 * REST WS UMA resource set description API
 * 
 * @author Yuriy Movchan Date: 10/04/2012
 */
public interface ResourceSetRegistrationService {
	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetStatus addResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid, ResourceSet resourceSet);

	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetStatus updateResourceSet(@HeaderParam("Authorization") String authorization, @HeaderParam("If-Match") String rsver, @PathParam("rsid") String rsid, ResourceSet resourceSet);

	@GET
	@Path("{rsid}")
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public VersionedResourceSet getResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

    /**
     * Gets resource set lists.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param p_scope scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public List<String> getResourceSetList(@HeaderParam("Authorization") String authorization, @QueryParam("scope") String p_scope);

	@DELETE
	@Path("{rsid}")
	public void deleteResourceSet(@HeaderParam("Authorization") String authorization, @HeaderParam("If-Match") String rsver, @PathParam("rsid") String rsid);
}
