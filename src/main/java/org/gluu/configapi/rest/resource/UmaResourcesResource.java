package org.gluu.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.UmaResourceService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.AttributeNames;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.uma.persistence.UmaResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.UMA + ApiConstants.RESOURCES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UmaResourcesResource extends BaseResource {


    private static final String UMA_RESOURCE = "Uma resource";
    @Inject
    UmaResourceService umaResourceService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response fetchUmaResources(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
                                      @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        final List<UmaResource> resources;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            resources = umaResourceService.findResources(pattern, 1000);
        } else {
            resources = umaResourceService.getAllResources(limit);
        }
        return Response.ok(resources).build();
    }

    @GET
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getUmaResourceByImun(@PathParam(value = ApiConstants.INUM) @NotNull String id) {
        UmaResource resource = umaResourceService.getResourceById(id);
        checkResourceNotNull(resource, UMA_RESOURCE);
        return Response.ok(resource).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response createUmaResource(@Valid UmaResource umaResource) {
        checkNotNull(umaResource.getName(), AttributeNames.NAME);
        checkNotNull(umaResource.getDescription(), AttributeNames.DESCRIPTION);
        String id = UUID.randomUUID().toString();
        umaResource.setInum(UUID.randomUUID().toString());
        umaResource.setDn(umaResourceService.getDnForResource(id));

        umaResourceService.addResource(umaResource);

        return Response.status(Response.Status.CREATED).entity(umaResource).build();

    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateUmaResource(@Valid UmaResource resource) {
        String inum = resource.getInum();
        checkNotNull(inum, AttributeNames.INUM);
        UmaResource existingResource = umaResourceService.getResourceById(inum);
        checkResourceNotNull(existingResource, UMA_RESOURCE);
        resource.setInum(existingResource.getInum());
        resource.setDn(umaResourceService.getDnForResource(inum));
        umaResourceService.updateResource(resource);
        return Response.ok(resource).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response patchResource(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) throws JsonPatchException, IOException {
        UmaResource existingResource = umaResourceService.getResourceById(inum);
        checkResourceNotNull(existingResource, UMA_RESOURCE);

        existingResource = Jackson.applyPatch(pathString, existingResource);
        umaResourceService.updateResource(existingResource);
        return Response.ok(existingResource).build();

    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response deleteUmaResource(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
        UmaResource umaResource = umaResourceService.getResourceById(inum);
        checkResourceNotNull(umaResource, UMA_RESOURCE);
        umaResourceService.remove(umaResource);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
