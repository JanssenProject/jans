/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;

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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@Path(ApiConstants.CONFIG + ApiConstants.JWKS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JwksResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    /**
     * Retrieve the server's configured JSON Web Key Set (JWKS).
     *
     * @return the HTTP Response containing the JWKS as a JSON string in the response entity
     */
    @Operation(summary = "Gets list of JSON Web Key (JWK) used by server", description = "Gets list of JSON Web Key (JWK) used by server", operationId = "get-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JWKS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response get() {
        final String json = configurationService.findConf().getWebKeys().toString();
        log.debug("JWKS json :{}", json);
        return Response.ok(json).build();
    }

    /**
     * Replace the server's JSON Web Key Set (JWKS) with the provided configuration.
     *
     * Replaces the existing web keys in the persisted configuration with the given WebKeysConfiguration and returns the updated JWKS as a JSON string.
     *
     * @param webkeys the new WebKeysConfiguration to store as the server's JWKS
     * @return the updated JWKS serialized as a JSON string
     */
    @Operation(summary = "Replaces JSON Web Keys", description = "Replaces JSON Web Keys", operationId = "put-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "JSON Web Keys object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/web-keys-all.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.JWKS_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response put(WebKeysConfiguration webkeys) {
        log.debug("JWKS details to be updated - webkeys:{}", webkeys);
        final Conf conf = configurationService.findConf();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    /**
     * Apply a JSON Patch to the stored Web Keys configuration and persist the change.
     *
     * @param requestString the JSON Patch document (media type application/json-patch+json) as a string
     * @return the updated WebKeysConfiguration serialized as a JSON string
     * @throws JsonPatchException if the patch cannot be applied to the existing configuration
     * @throws IOException if an I/O error occurs while processing the patch
     */
    @Operation(summary = "Patches JSON Web Keys", description = "Patches JSON Web Keys", operationId = "patch-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/web-keys-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.JWKS_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patch(String requestString) throws JsonPatchException, IOException {
        log.debug("JWKS details to be patched - requestString:{}", requestString);
        final Conf conf = configurationService.findConf();
        WebKeysConfiguration webKeys = conf.getWebKeys();
        webKeys = Jackson.applyPatch(requestString, webKeys);
        conf.setWebKeys(webKeys);
        configurationService.merge(conf);
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    /**
     * Add a new JSON Web Key (JWK) to the server's JWKS configuration.
     *
     * @param jwk the JSONWebKey to add
     * @return the created JSONWebKey
     * @throws NotAcceptableException if a key with the same `kid` already exists
     */
    @Operation(summary = "Configuration – JWK - JSON Web Key (JWK)", description = "Configuration – JWK - JSON Web Key (JWK)", operationId = "post-config-jwks-key", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "JSONWebKey object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class) , examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/jwks-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class) , examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "Not Acceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.JWKS_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.KEY_PATH)
    public Response getKeyById(@NotNull JSONWebKey jwk) {
        log.debug("Add a new Key to the JWKS:{}", jwk);
        Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        log.debug("WebKeysConfiguration before addding new key:{} ", webkeys);

        if (getJSONWebKey(webkeys, jwk.getKid()) != null) {
            throw new NotAcceptableException(
                    getNotAcceptableException("JWK with same kid - '" + jwk.getKid() + "' already exists!"));
        }

        // Add key
        webkeys.getKeys().add(jwk);
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        
        return Response.status(Response.Status.CREATED).entity(jwk).build();
    }

    /**
     * Retrieve a JSON Web Key by its key identifier (kid).
     *
     * @param kid the key identifier (kid) to look up; must not be null
     * @return the JSONWebKey with the given kid, or null if no matching key exists
     */
    @Operation(summary = "Get a JSON Web Key based on kid", description = "Get a JSON Web Key based on kid", operationId = "get-jwk-by-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JWKS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response getKeyById(@Parameter(description = "The unique identifier for the key") @PathParam(ApiConstants.KID) @NotNull String kid) {
        log.debug("Fetch JWK details by kid:{}", kid);
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        log.debug("WebKeysConfiguration before addding new key:{}", webkeys);
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        return Response.ok(jwk).build();
    }

    /**
     * Patch a JSON Web Key identified by its kid.
     *
     * @param kid           the unique identifier of the key to patch
     * @param requestString a JSON Patch document (application/json-patch+json) as a string
     * @return              the patched JSON Web Key
     * @throws JsonPatchException if the JSON Patch cannot be applied to the existing key
     * @throws IOException        if an I/O error occurs while processing the patch
     * @throws NotFoundException  if no key with the given kid exists
     */
    @Operation(summary = "Patch a specific JSON Web Key based on kid", description = "Patch a specific JSON Web Key based on kid", operationId = "patch-config-jwk-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/jwks-patch.json") ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-patch-response.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }, groupScopes = {}, 
    superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response patch(@Parameter(description = "The unique identifier for the key") @PathParam(ApiConstants.KID) @NotNull String kid, @NotNull String requestString)
            throws JsonPatchException, IOException {
        log.debug("JWKS details to be patched for kid:{}, requestString:{}", kid ,requestString);
        Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        if (jwk == null) {
            throw new NotFoundException(getNotFoundError("JWK with kid - '" + kid + "' does not exist!"));
        }

        // Patch
        jwk = Jackson.applyPatch(requestString, jwk);
        log.debug("JWKS details patched - jwk:{}", jwk);

        // Remove old Jwk
        conf.getWebKeys().getKeys().removeIf(x -> x.getKid() != null && x.getKid().equals(kid));
        log.debug("WebKeysConfiguration after removing old key:{}", conf.getWebKeys().getKeys());

        // Update
        conf.getWebKeys().getKeys().add(jwk);
        configurationService.merge(conf);

        return Response.ok(jwk).build();
    }

    /**
     * Delete the JSON Web Key identified by its kid.
     *
     * @param kid the unique identifier of the key to delete
     * @return a Response with HTTP 204 No Content when the key is successfully deleted
     * @throws NotFoundException if no key with the provided kid exists
     */
    @Operation(summary = "Delete a JSON Web Key based on kid", description = "Delete a JSON Web Key based on kid", operationId = "delete-config-jwk-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "Not Acceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_DELETE_ACCESS } , groupScopes = {}, 
    superScopes = { ApiAccessConstants.JWKS_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response deleteKey(@Parameter(description = "The unique identifier for the key") @PathParam(ApiConstants.KID) @NotNull String kid) {
        log.debug("Key to be to be deleted - kid:{}", kid);
        final Conf conf = configurationService.findConf();
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        if (jwk == null) {
            throw new NotFoundException(getNotFoundError("JWK with kid - '" + kid + "' does not exist!"));
        }

        conf.getWebKeys().getKeys().removeIf(x -> x.getKid() != null && x.getKid().equals(kid));
        configurationService.merge(conf);
        return Response.noContent().build();
    }

    private JSONWebKey getJSONWebKey(WebKeysConfiguration webkeys, String kid) {
        if (kid != null && webkeys.getKeys() != null && !webkeys.getKeys().isEmpty()) {
            return webkeys.getKeys().stream().filter(x -> x.getKid() != null && x.getKid().equals(kid)).findAny()
                    .orElse(null);
        }
        return null;
    }
}