/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.service.uma.ResourceSetService;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.UMA + ApiConstants.RESOURCES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UMAResource extends BaseResource {

	/**
	 * 
	 */
	private static final String UMA_RESOURCE = "Uma resource";
	@Inject
	private ResourceSetService umaResourcesService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response fetchUmaResources(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<UmaResource> resources = new ArrayList<UmaResource>();
		if (!pattern.isEmpty() && pattern.length() >= 2) {
			resources = umaResourcesService.findResources(pattern, 1000);
		} else {
			resources = umaResourcesService.getAllResources(limit);
		}
		return Response.ok(resources).build();
	}

	@GET
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUmaResourceByImun(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		String resourceDn = umaResourcesService.getDnForResource(inum);
		UmaResource resource = umaResourcesService.getResourceByDn(resourceDn);
		checkResourceNotNull(resource, UMA_RESOURCE);
		return Response.ok(resource).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createUmaResource(@Valid UmaResource umaResource) {
		checkNotNull(umaResource.getName(), AttributeNames.NAME);
		checkNotNull(umaResource.getDescription(), AttributeNames.DESCRIPTION);
		String inum = umaResourcesService.generateInumForNewResource();
		umaResource.setInum(inum);
		umaResource.setDn(umaResourcesService.getDnForResource(inum));
		umaResourcesService.addResource(umaResource);
		String dn = umaResourcesService.getDnForResource(inum);
		UmaResource result = umaResourcesService.getResourceByDn(dn);
		return Response.status(Response.Status.CREATED).entity(result).build();

	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUmaResource(@Valid UmaResource resource) {
		String inum = resource.getInum();
		checkNotNull(inum, AttributeNames.INUM);
		String dn = umaResourcesService.getDnForResource(inum);
		UmaResource existingResource = umaResourcesService.getResourceByDn(dn);
		checkResourceNotNull(existingResource, UMA_RESOURCE);
		resource.setInum(existingResource.getInum());
		resource.setDn(umaResourcesService.getDnForResource(inum));
		umaResourcesService.updateResource(resource);
		UmaResource result = umaResourcesService.getResourceByDn(dn);
		return Response.ok(result).build();
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response deleteUmaResource(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		String dn = umaResourcesService.getDnForResource(inum);
		UmaResource umaResource = umaResourcesService.getResourceByDn(dn);
		checkResourceNotNull(umaResource, UMA_RESOURCE);
		umaResourcesService.removeResource(umaResource);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

}
