/**
 *
 */
package org.gluu.configapi.rest.resource;

/**
 * @author Mougang T.Gasmyr
 *
 */

//@Path(ApiConstants.BASE_API_URL + ApiConstants.UMA + ApiConstants.SCOPES)
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public class UmaScopesResource extends BaseResource {
/*
	private static final String UMA_SCOPE = "Uma scope";

	@Inject
	Logger logger;

	@Inject
	UmaScopeService umaScopeService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAllUmaScopes(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<Scope> scopes = new ArrayList<Scope>();
		if (!pattern.isEmpty()) {
			scopes = umaScopeService.findUmaScopes(pattern, limit);
		} else {
			scopes = umaScopeService.getAllUmaScopes(limit);
		}
		return Response.ok(scopes).build();
	}

	@GET
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUmaScopeByImun(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		Scope scope = umaScopeService.getUmaScopeByInum(inum);
		checkResourceNotNull(scope, UMA_SCOPE);
		return Response.ok(scope).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createUmaScope(@Valid Scope scope) {
		checkNotNull(scope.getId(), AttributeNames.ID);
		checkNotNull(scope.getDisplayName(), AttributeNames.DISPLAY_NAME);
		String inum = umaScopeService.generateInumForNewScope();
		scope.setInum(inum);
		scope.setDn(umaScopeService.getDnForScope(inum));
		scope.setScopeType(ScopeType.UMA);
		umaScopeService.addUmaScope(scope);
		Scope result = umaScopeService.getUmaScopeByInum(inum);
		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUmaScope(@Valid Scope scope) {
		String inum = scope.getInum();
		checkNotNull(inum, AttributeNames.INUM);
		Scope existingScope = umaScopeService.getUmaScopeByInum(inum);
		checkResourceNotNull(existingScope, UMA_SCOPE);
		scope.setInum(existingScope.getInum());
		scope.setBaseDn(umaScopeService.getDnForScope(inum));
		scope.setScopeType(ScopeType.UMA);
		umaScopeService.updateUmaScope(scope);
		Scope result = umaScopeService.getUmaScopeByInum(inum);
		return Response.ok(result).build();
	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response patchUmaScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
		Scope existingScope = umaScopeService.getUmaScopeByInum(inum);
		checkResourceNotNull(existingScope, UMA_SCOPE);
		try {
			existingScope = Jackson.applyPatch(pathString, existingScope);
			umaScopeService.updateUmaScope(existingScope);
			return Response.ok(existingScope).build();
		} catch (Exception e) {
			logger.error("", e);
			throw new WebApplicationException(e.getMessage());
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response deleteUmaScope(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		Scope scope = umaScopeService.getUmaScopeByInum(inum);
		checkResourceNotNull(scope, UMA_SCOPE);
		umaScopeService.removeUmaScope(scope);
		return Response.status(Response.Status.NO_CONTENT).build();
	}
	*/
}
