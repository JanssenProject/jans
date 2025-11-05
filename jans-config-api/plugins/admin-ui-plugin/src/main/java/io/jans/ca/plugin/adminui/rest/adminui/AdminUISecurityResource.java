package io.jans.ca.plugin.adminui.rest.adminui;

import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUISecurityService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

@Path("/admin-ui/security")
public class AdminUISecurityResource {
    static final String POLICY_STORE = "policyStore";
    static final String SET_REMOTE_POLICY_STORE_AS_DEFAULT = "setRemotePolicyStoreAsDefault";
    static final String SYNC_ROLE_SCOPES_MAPPING = "/syncRoleScopesMapping";
    static final String SECURITY_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/security.readonly";
    static final String SECURITY_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/security.write";

    @Inject
    Logger log;

    @Inject
    AdminUISecurityService adminUISecurityService;

    @Operation(summary = "Get Admin UI policy store", description = "Get Admin UI policy store", operationId = "get-adminui-policy-store", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Get Admin UI policy store")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(POLICY_STORE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_READ}, groupScopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response getPolicyStore() {
        try {
            log.info("Get Admin UI policy store.");
            GenericResponse response = adminUISecurityService.getPolicyStore();
            log.info("Policy Store received.");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_CONFIG_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_CONFIG_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Set remote policy store as default policy store of Admin UI", description = "Set remote policy store as default policy store of Admin UI", operationId = "set-remote-policy-store-as-default", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Set remote policy store as default policy store of Admin UI")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(SET_REMOTE_POLICY_STORE_AS_DEFAULT)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response setRemotePolicyStoreAsDefault() {
        try {
            log.info("Set remote policy store as default policy store of Admin UI.");
            GenericResponse response = adminUISecurityService.setRemotePolicyStoreAsDefault();
            log.info("Set remote policy store as default policy store of Admin UI");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.REWRITING_DEFAULT_POLICY_STORE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.REWRITING_DEFAULT_POLICY_STORE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    @Operation(summary = "Sync role-to-scope mappings from the policy store", description = "Sync the role-to-scope mappings from the policy store. If a remote policy store URL is configured and enabled, the mappings will be generated from the remote policy store; otherwise, they will be generated from the default policy store.", operationId = "sync-role-to-scopes-mappings", tags = {
            "Admin UI - Cedarling"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SECURITY_WRITE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GenericResponse.class, description = "Sync Role-to-Scopes mapping from policy-store")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(SYNC_ROLE_SCOPES_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SECURITY_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response syncRoleScopeMapping() {
        try {
            log.info("Sync Role-to-Scopes mappings from the policy-store.");
            GenericResponse response = adminUISecurityService.syncRoleScopeMapping();
            log.info("Sync Role-to-Scopes mappings from the policy-store completed");
            return Response.ok(response).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }
}
