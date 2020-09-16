/**
 *
 */
package org.gluu.configapi.rest.resource;

/**
 * @author Mougang T.Gasmyr
 *
 */
//@Path(ApiConstants.BASE_API_URL + ApiConstants.OPENID + ApiConstants.SECTORS)
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
public class OIDSectorResource extends BaseResource {

    /**
     *
     */
    private static final String SECTOR_IDENTIFIER = "sector identifier";
/*
	@Inject
	Logger logger;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSectorIdentifiers(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<OxAuthSectorIdentifier> sectors = new ArrayList<OxAuthSectorIdentifier>();
		if (!pattern.isEmpty()) {
			sectors = sectorIdentifierService.searchSectorIdentifiers(pattern, limit);
		} else {
			sectors = sectorIdentifierService.getAllSectorIdentifiers();
		}
		return Response.ok().entity(sectors).build();
	}

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getSectorByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		OxAuthSectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
		checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
		return Response.ok().entity(sectorIdentifier).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createNewOpenIDSector(@Valid OxAuthSectorIdentifier sectorIdentifier) {
		checkNotNull(sectorIdentifier.getDescription(), AttributeNames.DESCRIPTION);
		String oxId = sectorIdentifierService.generateIdForNewSectorIdentifier();
		sectorIdentifier.setId(oxId);
		sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(oxId));
		sectorIdentifierService.addSectorIdentifier(sectorIdentifier);
		OxAuthSectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(oxId);
		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSector(@Valid OxAuthSectorIdentifier sectorIdentifier) {
		String inum = sectorIdentifier.getId();
		checkNotNull(inum, AttributeNames.INUM);
		checkNotNull(sectorIdentifier.getDescription(), AttributeNames.DESCRIPTION);
		OxAuthSectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
		checkResourceNotNull(existingSector, SECTOR_IDENTIFIER);
		sectorIdentifier.setId(existingSector.getId());
		sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(inum));
		sectorIdentifierService.updateSectorIdentifier(sectorIdentifier);
		OxAuthSectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(existingSector.getId());
		return Response.ok(result).build();
	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
		OxAuthSectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
		checkResourceNotNull(existingSector, SECTOR_IDENTIFIER);
		try {
			existingSector = Jackson.applyPatch(pathString, existingSector);
			sectorIdentifierService.updateSectorIdentifier(existingSector);
			return Response.ok(existingSector).build();
		} catch (Exception e) {
			logger.error("", e);
			throw new WebApplicationException(e.getMessage());
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteSector(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		OxAuthSectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
		checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
		sectorIdentifierService.removeSectorIdentifier(sectorIdentifier);
		return Response.noContent().build();

	}
*/
}
