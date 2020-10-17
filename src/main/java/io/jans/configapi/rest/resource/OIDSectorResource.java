/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.persistence.model.SectorIdentifier;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.SectorService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.Jackson;

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
 */
@Path(ApiConstants.OPENID + ApiConstants.SECTORS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OIDSectorResource extends BaseResource {

    private static final String SECTOR_IDENTIFIER = "sector identifier";

    @Inject
    SectorService sectorIdentifierService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getSectorIdentifiers(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        final List<SectorIdentifier> sectors;
        if (!pattern.isEmpty()) {
            sectors = sectorIdentifierService.searchSectorIdentifiers(pattern, limit);
        } else {
            sectors = sectorIdentifierService.getAllSectorIdentifiers();
        }
        return Response.ok().entity(sectors).build();
    }

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response getSectorByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        SectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
        return Response.ok().entity(sectorIdentifier).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response createNewOpenIDSector(@Valid SectorIdentifier sectorIdentifier) {
        checkNotNull(sectorIdentifier.getDescription(), AttributeNames.DESCRIPTION);
        String jsId = UUID.randomUUID().toString();
        sectorIdentifier.setId(jsId);
        sectorIdentifier.setBaseDn(sectorIdentifierService.getDnForSectorIdentifier(jsId));
        sectorIdentifierService.addSectorIdentifier(sectorIdentifier);
        SectorIdentifier result = sectorIdentifierService.getSectorIdentifierById(jsId);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
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
    @ProtectedApi(scopes = {WRITE_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) throws JsonPatchException, IOException {
        SectorIdentifier existingSector = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(existingSector, SECTOR_IDENTIFIER);
        existingSector = Jackson.applyPatch(pathString, existingSector);
        sectorIdentifierService.updateSectorIdentifier(existingSector);
        return Response.ok(existingSector).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response deleteSector(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        SectorIdentifier sectorIdentifier = sectorIdentifierService.getSectorIdentifierById(inum);
        checkResourceNotNull(sectorIdentifier, SECTOR_IDENTIFIER);
        sectorIdentifierService.removeSectorIdentifier(sectorIdentifier);
        return Response.noContent().build();
    }
}
