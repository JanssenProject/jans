/**
 *
 */
package org.gluu.configapi.rest.resource;

/**
 * @author Mougang T.Gasmyr
 *
 */

//@Path(ApiConstants.BASE_API_URL + ApiConstants.UMA + ApiConstants.RESOURCES)
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
public class UmaResourcesResource extends BaseResource {
/*

	private static final String UMA_RESOURCE = "Uma resource";
	@Inject
	ResourceSetService umaResourcesService;

	@Inject
	Logger logger;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response fetchUmaResources(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
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

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response patchResource(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
		String dn = umaResourcesService.getDnForResource(inum);
		UmaResource existingResource = umaResourcesService.getResourceByDn(dn);
		checkResourceNotNull(existingResource, UMA_RESOURCE);
		try {
			existingResource = Jackson.applyPatch(pathString, existingResource);
			umaResourcesService.updateResource(existingResource);
			return Response.ok(existingResource).build();
		} catch (Exception e) {
			logger.error("", e);
			throw new WebApplicationException(e.getMessage());
		}
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
*/
}
