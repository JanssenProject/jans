/**
 *
 */
package org.gluu.configapi.rest.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.SectorService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.AttributeNames;
import org.gluu.configapi.util.Jackson;
import org.oxauth.persistence.model.SectorIdentifier;
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
    Logger logger;

    @Inject
    SectorService sectorIdentifierService;

    @GET
    @ProtectedApi(scopes = { READ_ACCESS })
    public Response getSectorIdentifiers(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        List<SectorIdentifier> sectors = new ArrayList<SectorIdentifier>();
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
        SectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
        return Response.ok().entity(sectorIdentifier).build();
    }

    @POST
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response createNewOpenIDSector(@Valid SectorIdentifier sectorIdentifier) {
        checkNotNull(sectorIdentifier.getDescription(), AttributeNames.DESCRIPTION);
        String oxId = sectorIdentifierService.generateIdForNewSectorIdentifier();
        sectorIdentifier.setId(oxId);
        sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(oxId));
        sectorIdentifierService.addSectorIdentifier(sectorIdentifier);
        SectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(oxId);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response updateSector(@Valid SectorIdentifier sectorIdentifier) {
        String inum = sectorIdentifier.getId();
        checkNotNull(inum, AttributeNames.INUM);
        checkNotNull(sectorIdentifier.getDescription(), AttributeNames.DESCRIPTION);
        SectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(existingSector, SECTOR_IDENTIFIER);
        sectorIdentifier.setId(existingSector.getId());
        sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(inum));
        sectorIdentifierService.updateSectorIdentifier(sectorIdentifier);
        SectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(existingSector.getId());
        return Response.ok(result).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
        SectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
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
        SectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
        sectorIdentifierService.removeSectorIdentifier(sectorIdentifier);
        return Response.noContent().build();

    }
}
