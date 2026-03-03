/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.ConnectionStatus;
import io.jans.configapi.core.util.Jackson;
import io.jans.model.ldap.GluuLdapConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.LDAP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LdapConfigurationResource extends ConfigBaseResource {

    @Inject
    LdapConfigurationService ldapConfigurationService;

    @Inject
    ConnectionStatus connectionStatus;

    /**
     * Retrieves all LDAP configurations.
     *
     * @return a list of GluuLdapConfiguration objects; empty list if none exist.
     */
    @Operation(summary = "Gets list of existing LDAP configurations.", description = "Gets list of existing LDAP configurations.", operationId = "get-config-database-ldap", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GluuLdapConfiguration.class)), examples = @ExampleObject(name = "Response json example", value = "example/auth/database/ldap/ldap-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getLdapConfiguration() {
        List<GluuLdapConfiguration> ldapConfigurationList = this.ldapConfigurationService.findLdapConfigurations();
        return Response.ok(ldapConfigurationList).build();
    }

    /**
     * Retrieves the LDAP configuration identified by the given name.
     *
     * @param name the LDAP configuration name to retrieve
     * @return the matching GluuLdapConfiguration
     * @throws javax.ws.rs.NotFoundException if no configuration with the given name exists
     */
    @Operation(summary = "Gets an LDAP configuration by name.", description = "Gets an LDAP configuration by name.", operationId = "get-config-database-ldap-by-name", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/database/ldap/ldap.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getLdapConfigurationByName(@Parameter(description = "Name of LDAP configuration") @PathParam(ApiConstants.NAME) String name) {
        GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);
        return Response.ok(ldapConfiguration).build();
    }

    /**
     * Create a new LDAP configuration.
     *
     * Attempts to persist the provided GluuLdapConfiguration and returns the created resource.
     *
     * @param ldapConfiguration the LDAP configuration to create
     * @return a Response containing the created GluuLdapConfiguration with HTTP status 201 (Created)
     * @throws NotAcceptableException if a configuration with the same configId already exists
     */
    @Operation(summary = "Adds a new LDAP configuration", description = "Adds a new LDAP configuration", operationId = "post-config-database-ldap", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "GluuLdapConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/database/ldap/ldap.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/database/ldap/ldap.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "Not Acceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.DATABASE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response addLdapConfiguration(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        logger.debug("LDAP configuration to be added - ldapConfiguration:{} ", ldapConfiguration);
        // Ensure that an LDAP server with same name does not exists.
        try {
            ldapConfiguration = findLdapConfigurationByName(ldapConfiguration.getConfigId());
            logger.error("Ldap Configuration with same name:{}  already exists!", ldapConfiguration.getConfigId());

            throw new NotAcceptableException(getNotAcceptableException(
                    "Ldap Configuration with same name - '" + ldapConfiguration.getConfigId() + "' already exists!"));
        } catch (NotFoundException ne) {
            this.ldapConfigurationService.save(ldapConfiguration);
            ldapConfiguration = findLdapConfigurationByName(ldapConfiguration.getConfigId());
            return Response.status(Response.Status.CREATED).entity(ldapConfiguration).build();
        }
    }

    /**
     * Update an existing LDAP configuration.
     *
     * @param ldapConfiguration the LDAP configuration to persist; its `configId` must match an existing configuration
     * @return the updated {@link GluuLdapConfiguration}
     */
    @Operation(summary = "Updates LDAP configuration", description = "Updates LDAP configuration", operationId = "put-config-database-ldap", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "GluuLdapConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/database/ldap/ldap.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/database/ldap/ldap.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.DATABASE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateLdapConfiguration(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        logger.debug("LDAP configuration to be updated - ldapConfiguration:{}", ldapConfiguration);
        findLdapConfigurationByName(ldapConfiguration.getConfigId());
        this.ldapConfigurationService.update(ldapConfiguration);
        return Response.ok(ldapConfiguration).build();
    }

    /**
     * Deletes the LDAP configuration with the given name.
     *
     * @param name the name of the LDAP configuration to delete
     * @return a Response with HTTP 204 No Content on success
     * @throws NotFoundException if no configuration with the given name exists
     */
    @Operation(summary = "Deletes an LDAP configuration", description = "Deletes an LDAP configuration", operationId = "delete-config-database-ldap-by-name", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_DELETE_ACCESS } , groupScopes = {}, 
    superScopes = { ApiAccessConstants.DATABASE_LDAP_DELETE_ACCESS, ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name) {
        logger.debug("LDAP configuration to be deleted - name:{}", name);
        findLdapConfigurationByName(name);

        logger.info("Deleting Ldap Configuration by name:{}", name);
        this.ldapConfigurationService.remove(name);
        return Response.noContent().build();
    }

    /**
     * Applies a JSON Patch to the LDAP configuration identified by the given name and persists the updated configuration.
     *
     * @param name the identifier of the LDAP configuration to patch
     * @param requestString the JSON Patch payload as a string
     * @return the updated GluuLdapConfiguration
     * @throws JsonPatchException if the patch cannot be applied to the configuration
     * @throws IOException if an I/O error occurs while processing the patch
     */
    @Operation(summary = "Patches a LDAP configuration by name", description = "Patches a LDAP configuration by name", operationId = "patch-config-database-ldap-by-name", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/database/ldap/ldap-patch")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/database/ldap/ldap.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.DATABASE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchLdapConfigurationByName(@Parameter(description = "Name of LDAP configuration") @PathParam(ApiConstants.NAME) String name,
            @NotNull String requestString) throws JsonPatchException, IOException {
        logger.debug("LDAP configuration to be patched - name:{}, requestString:{} ", name, requestString);
        GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);

        logger.info("Patch Ldap Configuration by name:{} ", name);
        ldapConfiguration = Jackson.applyPatch(requestString, ldapConfiguration);
        this.ldapConfigurationService.update(ldapConfiguration);
        return Response.ok(ldapConfiguration).build();
    }

    /**
     * Check whether the provided LDAP configuration can establish a connection to its LDAP server.
     *
     * @param ldapConfiguration the LDAP configuration to test connectivity for
     * @return HTTP 200 response containing `true` if a connection to the LDAP server can be established using the provided configuration, `false` otherwise
     */
    @Operation(summary = "Tests an LDAP configuration", description = "Tests an LDAP configuration", operationId = "post-config-database-ldap-test", tags = {
            "Database - LDAP configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @RequestBody(description = "GluuLdapConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuLdapConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/database/ldap/ldap-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(name = "status", type = "boolean", description = "boolean value true if successful"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.DATABASE_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response testLdapConfigurationByName(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        logger.debug("LDAP configuration to be tested - ldapConfiguration:{}", ldapConfiguration);

        boolean status = connectionStatus.isUp(ldapConfiguration);
        logger.info("LdapConfigurationResource:::testLdapConfigurationByName() - status:{}", status);
        return Response.ok(status).build();
    }

    private GluuLdapConfiguration findLdapConfigurationByName(String name) {
        try {
            return this.ldapConfigurationService.findByName(name);
        } catch (NoSuchElementException ex) {
            logger.error("Could not find Ldap Configuration by name '" + name + "'", ex);
            throw new NotFoundException(getNotFoundError("Ldap Configuration - '" + name + "'"));
        }
    }
}