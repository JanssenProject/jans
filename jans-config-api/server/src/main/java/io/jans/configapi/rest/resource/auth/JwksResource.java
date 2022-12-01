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

    @Operation(summary = "Gets list of JSON Web Key (JWK) used by server", description = "Gets list of JSON Web Key (JWK) used by server", operationId = "get-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class) , examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JWKS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response get() {
        final String json = configurationService.findConf().getWebKeys().toString();
        log.debug("JWKS json :{}", json);
        return Response.ok(json).build();
    }

    @Operation(summary = "Replaces JSON Web Keys", description = "Replaces JSON Web Keys", operationId = "put-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_WRITE_ACCESS}))
    @RequestBody(description = "JSON Web Keys object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/web-keys-all.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class) , examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response put(WebKeysConfiguration webkeys) {
        log.debug("JWKS details to be updated - webkeys:{}", webkeys);
        final Conf conf = configurationService.findConf();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    @Operation(summary = "Patches JSON Web Keys", description = "Patches JSON Web Keys", operationId = "patch-config-jwks", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_WRITE_ACCESS }))
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/web-keys-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebKeysConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/web-keys-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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

    @Operation(summary = "Configuration – JWK - JSON Web Key (JWK)", description = "Configuration – JWK - JSON Web Key (JWK)", operationId = "post-config-jwks-key", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_WRITE_ACCESS }))
    @RequestBody(description = "JSONWebKey object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class) , examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/jwks-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class) , examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "Not Acceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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

    @Operation(summary = "Get a JSON Web Key based on kid", description = "Get a JSON Web Key based on kid", operationId = "get-jwk-by-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_READ_ACCESS  }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_READ_ACCESS } , groupScopes = {
            ApiAccessConstants.JWKS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response getKeyById(@PathParam(ApiConstants.KID) @NotNull String kid) {
        log.debug("Fetch JWK details by kid:{}", kid);
        WebKeysConfiguration webkeys = configurationService.findConf().getWebKeys();
        log.debug("WebKeysConfiguration before addding new key:{}", webkeys);
        JSONWebKey jwk = getJSONWebKey(webkeys, kid);
        return Response.ok(jwk).build();
    }

    @Operation(summary = "Patch a specific JSON Web Key based on kid", description = "Patch a specific JSON Web Key based on kid", operationId = "patch-config-jwk-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_WRITE_ACCESS }))
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/jwks/jwks-patch.json") ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JSONWebKey.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/jwks/jwks-patch-response.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_WRITE_ACCESS }, groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response patch(@PathParam(ApiConstants.KID) @NotNull String kid, @NotNull String requestString)
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

    @Operation(summary = "Delete a JSON Web Key based on kid", description = "Delete a JSON Web Key based on kid", operationId = "delete-config-jwk-kid", tags = {
            "Configuration – JWK - JSON Web Key (JWK)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JWKS_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "Not Acceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.JWKS_DELETE_ACCESS } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.KID_PATH)
    public Response deleteKey(@PathParam(ApiConstants.KID) @NotNull String kid) {
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
