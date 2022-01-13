/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaResource;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.model.uma.UmaResourceWithId;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * REST WS UMA resource set description API
 *
 * @author Yuriy Zabrovarnyy
 */
public interface UmaResourceService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaResourceResponse addResource(@HeaderParam("Authorization") String authorization, UmaResource resource);

    @PUT
    @Path("{rsid}")
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaResourceResponse updateResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid, UmaResource resource);

    @GET
    @Path("{rsid}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaResourceWithId getResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);

    /**
     * Gets resources.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param scope         scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    List<String> getResourceList(@HeaderParam("Authorization") String authorization, @QueryParam("scope") String scope);

    @DELETE
    @Path("{rsid}")
    void deleteResource(@HeaderParam("Authorization") String authorization, @PathParam("rsid") String rsid);
}
