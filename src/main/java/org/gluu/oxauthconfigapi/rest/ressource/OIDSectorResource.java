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

import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.model.OxAuthSectorIdentifier;
import org.gluu.oxtrust.service.SectorIdentifierService;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.OPENID + ApiConstants.SECTORS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OIDSectorResource extends BaseResource {

	/**
	 * 
	 */
	private static final String SECTOR_IDENTIFIER = "sector identifier";
	@Inject
	SectorIdentifierService sectorIdentifierService;
	@Inject
	Logger logger;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSectorIdentifiers(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
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

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteSector(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		OxAuthSectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
		checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
		sectorIdentifierService.removeSectorIdentifier(sectorIdentifier);
		return Response.noContent().build();

	}

}
