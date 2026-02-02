package io.jans.configapi.plugin.shibboleth.rest;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfiguration;
import io.jans.configapi.plugin.shibboleth.model.TrustedServiceProvider;
import io.jans.configapi.plugin.shibboleth.service.ShibbolethService;
import io.jans.configapi.plugin.shibboleth.util.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethResource {

    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;

    @Operation(summary = "Gets Shibboleth IDP configuration", description = "Gets Shibboleth IDP configuration", operationId = "get-shibboleth-config", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shibboleth IDP configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethIdpConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/config")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_READ_ACCESS })
    public Response getConfiguration() {
        logger.debug("GET /shibboleth/config");
        ShibbolethIdpConfiguration config = shibbolethService.getConfiguration();
        return Response.ok(config).build();
    }

    @Operation(summary = "Updates Shibboleth IDP configuration", description = "Updates Shibboleth IDP configuration", operationId = "put-shibboleth-config", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Shibboleth IDP configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethIdpConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @PUT
    @Path("/config")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_WRITE_ACCESS })
    public Response updateConfiguration(@Valid @NotNull ShibbolethIdpConfiguration configuration) {
        logger.info("PUT /shibboleth/config");
        shibbolethService.updateConfiguration(configuration);
        return Response.ok(configuration).build();
    }

    @Operation(summary = "Gets trusted service providers", description = "Gets list of trusted SAML service providers", operationId = "get-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trusted service providers", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustedServiceProvider.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/trust")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_READ_ACCESS })
    public Response getTrustedServiceProviders() {
        logger.debug("GET /shibboleth/trust");
        List<TrustedServiceProvider> providers = shibbolethService.getTrustedServiceProviders();
        return Response.ok(providers).build();
    }

    @Operation(summary = "Gets a trusted service provider", description = "Gets a trusted SAML service provider by entity ID", operationId = "get-shibboleth-trust-by-id", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trusted service provider", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustedServiceProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/trust/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_READ_ACCESS })
    public Response getTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId) {
        logger.debug("GET /shibboleth/trust/{}", entityId);
        TrustedServiceProvider provider = shibbolethService.getTrustedServiceProvider(entityId);

        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(provider).build();
    }

    @Operation(summary = "Adds trusted service provider", description = "Adds a new trusted SAML service provider", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created trusted service provider", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustedServiceProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Conflict - Entity already exists"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @POST
    @Path("/trust")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_WRITE_ACCESS })
    public Response addTrustedServiceProvider(@Valid @NotNull TrustedServiceProvider serviceProvider) {
        logger.info("POST /shibboleth/trust");

        if (serviceProvider.getEntityId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Entity ID is required")
                    .build();
        }

        TrustedServiceProvider existing = shibbolethService.getTrustedServiceProvider(serviceProvider.getEntityId());
        if (existing != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Service Provider with this entity ID already exists")
                    .build();
        }

        shibbolethService.addTrustedServiceProvider(serviceProvider);
        return Response.status(Response.Status.CREATED).entity(serviceProvider).build();
    }

    @Operation(summary = "Updates trusted service provider", description = "Updates an existing trusted SAML service provider", operationId = "put-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated trusted service provider", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustedServiceProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @PUT
    @Path("/trust/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_WRITE_ACCESS })
    public Response updateTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId,
            @Valid @NotNull TrustedServiceProvider serviceProvider) {
        logger.info("PUT /shibboleth/trust/{}", entityId);

        TrustedServiceProvider existing = shibbolethService.getTrustedServiceProvider(entityId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        serviceProvider.setEntityId(entityId);
        shibbolethService.updateTrustedServiceProvider(serviceProvider);
        return Response.ok(serviceProvider).build();
    }

    @Operation(summary = "Deletes trusted service provider", description = "Deletes a trusted SAML service provider", operationId = "delete-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @DELETE
    @Path("/trust/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_WRITE_ACCESS })
    public Response deleteTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId) {
        logger.info("DELETE /shibboleth/trust/{}", entityId);

        TrustedServiceProvider existing = shibbolethService.getTrustedServiceProvider(entityId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        shibbolethService.deleteTrustedServiceProvider(entityId);
        return Response.noContent().build();
    }

    @Operation(summary = "Gets IDP metadata", description = "Gets Shibboleth IDP SAML metadata", operationId = "get-shibboleth-metadata", tags = {
            "Shibboleth IDP - Config Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SHIBBOLETH_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IDP SAML metadata", content = @Content(mediaType = MediaType.APPLICATION_XML)),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "501", description = "Not Implemented"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_XML)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_READ_ACCESS })
    public Response getIdpMetadata() {
        logger.debug("GET /shibboleth/metadata");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
