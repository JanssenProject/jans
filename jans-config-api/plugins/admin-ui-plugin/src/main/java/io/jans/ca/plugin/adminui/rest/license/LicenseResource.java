package io.jans.ca.plugin.adminui.rest.license;

import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.model.auth.SSARequest;
import io.jans.ca.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

@Path("/admin-ui/license")
public class LicenseResource {

    static final String IS_ACTIVE = "/isActive";
    static final String ACTIVATE = "/activate";
    static final String TRIAL = "/trial";
    static final String DETAILS = "/details";
    static final String RETRIEVE = "/retrieve";
    static final String SSA = "/ssa";
    static final String IS_LICENSE_CONFIG_VALID = "/isConfigValid";
    static final String CONFIG_DELETE = "/deleteConfig";

    public static final String SCOPE_LICENSE_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly";
    public static final String SCOPE_LICENSE_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/license.write";

    @Inject
    Logger log;

    @Inject
    LicenseDetailsService licenseDetailsService;

    @Operation(summary = "Check if admin-ui license is active", description = "Check if admin-ui license is active", operationId = "is-license-active", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(IS_ACTIVE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response isActive() {
        GenericResponse licenseResponse = null;
        try {
            log.info("Check if active license present.");
            licenseResponse = licenseDetailsService.checkLicense();
            log.info("Active license present (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Delete license details in admin-ui configuration", description = "Delete license details in admin-ui configuration", operationId = "license-config-delete", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_WRITE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @DELETE
    @Path(CONFIG_DELETE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_WRITE}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLicenseConfiguration() {
        GenericResponse licenseResponse = null;
        try {
            log.info("Before deleting license configuration.");
            licenseResponse = licenseDetailsService.deleteLicenseConfiguration();
            log.info("License deleted successful (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.LICENSE_DELETE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Retrieve license from SCAN", description = "Retrieve license from SCAN", operationId = "retrieve-license", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(RETRIEVE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveLicense() {
        GenericResponse licenseResponse = null;
        try {
            log.info("Retrieve license from SCAN.");
            licenseResponse = licenseDetailsService.retrieveLicense();
            log.info("Retrieve license from SCAN result (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Generate trial license", description = "Generate trial license", operationId = "get-trial-license", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(TRIAL)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response trial() {
        GenericResponse licenseResponse = null;
        try {
            log.info("Generate trial license.");
            licenseResponse = licenseDetailsService.generateTrialLicense();
            log.info("Generate trial license (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
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
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @POST
    @Path(ACTIVATE)
    @ProtectedApi(scopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response activate(@Valid @NotNull LicenseRequest licenseRequest) {
        GenericResponse licenseResponse = null;
        try {
            log.info("Trying to activate license using licese-key.");
            licenseResponse = licenseDetailsService.activateLicense(licenseRequest);
            log.info("License activated (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Save SSA in configuration", description = "Save SSA in configuration", operationId = "adminui-post-ssa", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_WRITE}))
    @RequestBody(description = "SSARequest object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SSARequest.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @POST
    @Path(SSA)
    @ProtectedApi(scopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response ssa(@Valid @NotNull SSARequest ssaRequest) {
        GenericResponse licenseResponse = null;
        try {
            log.info("Trying to execute post ssa.");
            licenseResponse = licenseDetailsService.postSSA(ssaRequest);
            log.info("SSA Saved (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Is license configuration valid", description = "Is license configuration valid", operationId = "check-adminui-license-config", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(IS_LICENSE_CONFIG_VALID)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response isConfigValid() {
        GenericResponse licenseResponse = null;
        try {
            log.info("Check if license config valid.");
            licenseResponse = licenseDetailsService.validateLicenseConfiguration();
            log.info("License config valid (true/false): {}", licenseResponse.isSuccess());
            return Response.status(licenseResponse.getResponseCode()).entity(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @Operation(summary = "Get admin ui license details", description = "Get admin ui license details", operationId = "get-adminui-license", tags = {
            "Admin UI - License"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_LICENSE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LicenseResponse.class, description = "License Response"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "License response")))})
    @GET
    @Path(DETAILS)
    @ProtectedApi(scopes = {SCOPE_LICENSE_READ}, groupScopes = {SCOPE_LICENSE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response details() {
        try {
            log.info("Trying to fetch license details.");
            LicenseResponse licenseResponse = licenseDetailsService.getLicenseDetails();
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription(), e);
            return Response.serverError().entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription())).build();
        }
    }
}