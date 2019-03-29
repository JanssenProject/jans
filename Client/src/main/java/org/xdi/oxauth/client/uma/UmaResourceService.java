/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.UmaResource;
import org.xdi.oxauth.model.uma.UmaResourceResponse;
import org.xdi.oxauth.model.uma.UmaResourceWithId;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.*;
import java.util.List;

/**
 * REST WS UMA resource set description API
 *
 * @author Yuriy Zabrovarnyy
 */
public interface UmaResourceService {

	@POST
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	UmaResourceResponse addResource(@HeaderParam("Authorization") String authorization, UmaResource resource);

	@PUT
	@Path("{rsid}")
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE})
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	UmaResourceResponse updateResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid, UmaResource resource);

	@GET
	@Path("{rsid}")
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	UmaResourceWithId getResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

    /**
     * Gets resources.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param scope scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	List<String> getResourceList(@HeaderParam("Authorization") String authorization, @QueryParam("scope") String scope);

	@DELETE
	@Path("{rsid}")
	void deleteResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);
}
