/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.model.OxAuthClient;
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

	@Inject
	SectorIdentifierService sectorIdentifierService;
	@Inject
	Logger logger;

	@GET
	@Operation(summary = "Get list of OpenID Connect Sectors")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthSectorIdentifier[].class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSectorIdentifiers(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			List<OxAuthSectorIdentifier> sectors = new ArrayList<OxAuthSectorIdentifier>();
			if (!pattern.isEmpty()) {
				sectors = sectorIdentifierService.searchSectorIdentifiers(pattern, limit);
			} else {
				sectors = sectorIdentifierService.getAllSectorIdentifiers();
			}
			return Response.ok().entity(sectors).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@GET
	@Operation(summary = "Get OpenID Connect Sector by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthSectorIdentifier.class, required = true))),
			@APIResponse(responseCode = "404", description = "Resource not found"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getSectorByInum(@PathParam(ApiConstants.INUM) String inum) {
		try {
			OxAuthSectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
			if (inum == null || sectorIdentifier == null) {
				return getResourceNotFoundError();
			}
			return Response.ok().entity(sectorIdentifier).build();
		} catch (Exception e) {
			logger.error("Failed to fetch  OpenId Client Connect" + inum, e);
			return getInternalServerError(e);
		}
	}

	@POST
	@Operation(summary = "Create new OpenID Connect Sector")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = OxAuthSectorIdentifier.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createNewOpenIDSector(@Valid OxAuthSectorIdentifier sectorIdentifier) {
		try {
			if (sectorIdentifier.getDescription() == null) {
				return getMissingAttributeError(AttributeNames.DESCRIPTION);
			}
			String oxId = sectorIdentifierService.generateIdForNewSectorIdentifier();
			sectorIdentifier.setId(oxId);
			sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(oxId));
			sectorIdentifierService.addSectorIdentifier(sectorIdentifier);
			OxAuthSectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(oxId);
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			logger.error("Error encounter while saving the openid connect sector.", e);
			return getInternalServerError(e);
		}
	}

	@PUT
	@Operation(summary = "Update OpenId Connect Sector")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSector(@Valid OxAuthSectorIdentifier sectorIdentifier) {
		try {
			String inum = sectorIdentifier.getId();
			if (inum == null) {
				return getMissingAttributeError(AttributeNames.INUM);
			}
			if (sectorIdentifier.getDescription() == null) {
				return getMissingAttributeError(AttributeNames.DESCRIPTION);
			}
			OxAuthSectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
			if (existingSector != null) {
				sectorIdentifier.setId(existingSector.getId());
				sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(inum));
				sectorIdentifierService.updateSectorIdentifier(sectorIdentifier);
				OxAuthSectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(existingSector.getId());
				return Response.ok(result).build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to update OpenId Connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@DELETE
	@Operation(summary = "Delete OpenID Connect Sector")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteSector(@PathParam(ApiConstants.INUM) String inum) {
		try {
			OxAuthSectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
			if (inum == null || sectorIdentifier == null) {
				return getResourceNotFoundError();
			}
			sectorIdentifierService.removeSectorIdentifier(sectorIdentifier);
			return Response.noContent().build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}

	}

}
