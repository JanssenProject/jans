/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.ResourceSetWithId;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.*;
import java.util.List;

/**
 * REST WS UMA resource set description API
 *
 * @author Yuriy Zabrovarnyy
 */
public interface ResourceSetRegistrationService {

	@POST
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetResponse addResourceSet(@HeaderParam("Authorization") String authorization, ResourceSet resourceSet);

	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetResponse updateResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid, ResourceSet resourceSet);

	@GET
	@Path("{rsid}")
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ResourceSetWithId getResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

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
	public void deleteResourceSet(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);
}
