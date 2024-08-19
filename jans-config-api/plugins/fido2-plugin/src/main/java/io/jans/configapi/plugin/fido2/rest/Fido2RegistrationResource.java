package io.jans.configapi.plugin.fido2.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2RegistrationService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.fido2.Fido2DeviceData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.List;

@Path(Constants.REGISTRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2RegistrationResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    Fido2RegistrationService fido2RegistrationService;

    @Operation(summary = "Get details of connected FIDO2 devices registered to user", description = "Get details of connected FIDO2 devices registered to user", operationId = "get-registration-entries-fido2", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Fido2RegistrationEntry.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.ENTRIES + ApiConstants.USERNAME_PATH)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_READ_ACCESS })
    public Response findAllRegisteredByUsername(
            @Parameter(description = "User name") @PathParam("username") @NotNull String username) {
        logger.debug("FIDO2 registration entries by username.");
        List<Fido2RegistrationEntry> entries = fido2RegistrationService.findAllRegisteredByUsername(username);
        return Response.ok(entries).build();
    }

    @Operation(summary = "Delete Fido2 Device Data based on user name and device UID", description = "Delete Fido2 Device Data based on user name and device UID", operationId = "delete-fido2-device-by-username-uid", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(Constants.USER_DEVICE + Constants.USERNAME_PATH + Constants.UID_PATH)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_DELETE_ACCESS })
    public Response deleteFido2DeviceByUserAndDevice(
            @Parameter(description = "User name") @PathParam("username") @NotNull String username,
            @Parameter(description = "Unique identifier string (UUID) assigned to device.") @PathParam("uid") @NotNull String uid) {
        logger.debug("Request to delete Fido2 device identified by username:{}, uid:{}", username, uid);

        // delete device
        fido2RegistrationService.removeFido2DeviceData(username, uid);

        return Response.noContent().build();
    }

    @Operation(summary = "Delete Fido2 Device Data based on device UID", description = "Delete Fido2 Device Data based on device UID", operationId = "delete-fido2-device-data", tags = {
            "Fido2 - Registration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.FIDO2_CONFIG_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(Constants.DEVICE + Constants.UID_PATH)
    @ProtectedApi(scopes = { Constants.FIDO2_CONFIG_DELETE_ACCESS })
    public Response deleteFido2DeviceData(
            @Parameter(description = "Unique identifier string (UUID) assigned to device.") @PathParam("uid") @NotNull String uid) {
        logger.debug("Request to delete Fido2 device identified by uid:{}", uid);

        // delete device
        fido2RegistrationService.removeFido2DeviceData(uid);

        return Response.noContent().build();
    }
}
