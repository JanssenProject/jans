package io.jans.ca.plugin.adminui.rest.license;

import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.model.auth.LicenseApiResponse;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.configapi.util.ApiAccessConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin-ui/license")
public class LicenseResource {

    static final String IS_ACTIVE = "/isActive";
    static final String SAVE_API_CREDENTIALS = "/saveApiCredentials";
    static final String ACTIVATE_LICENSE = "/activateLicense";
    static final String LICENSE_DETAILS = "/licenseDetails";

    public static final String SCOPE_OPENID = "openid";
    static final String SCOPE_LICENSE_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly";
    static final String SCOPE_LICENSE_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/license.write";

    @Inject
    Logger log;

    @Inject
    LicenseDetailsService licenseDetailsService;

    @Operation(summary = "Check if admin-ui license is active", description = "Check if admin-ui license is active", operationId = "is-license-active", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response")))})
    @GET
    @Path(IS_ACTIVE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = { AppConstants.SCOPE_ADMINUI_READ })
    @Produces(MediaType.APPLICATION_JSON)
    public Response isActive() {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Check if active license present.");
            licenseResponse = licenseDetailsService.checkLicense();
            log.info("Active license present (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Activate license using license-key", description = "Activate license using license-key", operationId = "activate-adminui-license", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_WRITE}))
    @RequestBody(description = "LicenseRequest object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseRequest.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response")))})
    @POST
    @Path(ACTIVATE_LICENSE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_WRITE}, superScopes = { AppConstants.SCOPE_ADMINUI_WRITE })
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateLicense(@Valid @NotNull LicenseRequest licenseRequest) {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Trying to activate license using licese-key.");
            licenseResponse = licenseDetailsService.activateLicense(licenseRequest);
            log.info("License activated (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Save license api credentials", description = "Save license api credentials", operationId = "save-license-api-credentials", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_WRITE}))
    @RequestBody(description = "LicenseSpringCredentials object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseSpringCredentials.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseApiResponse.class, description = "License response")))})
    @POST
    @Path(SAVE_API_CREDENTIALS)
    @ProtectedApi(scopes = {SCOPE_LICENSE_WRITE}, superScopes = { AppConstants.SCOPE_ADMINUI_WRITE })
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveLicenseCredentials(@Valid @NotNull LicenseSpringCredentials licenseSpringCredentials) {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Trying to save license-spring credentials.");
            licenseResponse = licenseDetailsService.saveLicenseSpringCredentials(licenseSpringCredentials);
            log.info("License saved (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_LICENSE_SPRING_CREDENTIALS_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Get admin ui license details", description = "Get admin ui license details", operationId = "get-adminui-license", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseResponse.class, description = "License Response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(LICENSE_DETAILS)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = { AppConstants.SCOPE_ADMINUI_READ })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLicenseDetails() {
        try {
            log.info("Trying to fetch license details.");
            LicenseResponse licenseResponse = licenseDetailsService.getLicenseDetails();
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription(), e);
            return Response.serverError().entity(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription()).build();
        }
    }
}