package io.jans.ca.plugin.adminui.rest.adminui;

import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.auth.AppConfigResponse;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.adminui.AdminUIService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@Path("/admin-ui")
public class AdminUIResource {
    static final String CONFIG = "/config";
    static final String ROLES = "/adminUIRoles";
    static final String ROLE_PATH_VARIABLE = "/{adminUIRole}";
    static final String ROLE_CONST = "adminUIRole";
    static final String PERMISSIONS = "/adminUIPermissions";
    static final String PERMISSION_PATH_VARIABLE = "/{adminUIPermission}";
    static final String PERMISSION_CONST = "adminUIPermission";
    static final String ROLE_PERMISSIONS_MAPPING = "/adminUIRolePermissionsMapping";
    static final String SCOPE_ROLE_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly";
    static final String SCOPE_ROLE_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write";
    static final String SCOPE_ROLE_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.delete";
    static final String SCOPE_PERMISSION_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly";
    static final String SCOPE_PERMISSION_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write";
    static final String SCOPE_PERMISSION_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.delete";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.delete";
    public static final String ADMINUI_CONF_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/properties.readonly";
    public static final String ADMINUI_CONF_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/properties.write";

    //Admin Scopes
    static final String SCOPE_ROLE_ADMIN = "https://jans.io/oauth/config/adminui/role.admin";
    static final String SCOPE_PERMISSION_ADMIN = "https://jans.io/oauth/config/adminui/permission.admin";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_ADMIN = "https://jans.io/oauth/config/adminui/rolePermissionMapping.admin";
    
    
    @Inject
    Logger log;

    @Inject
    AdminUIService adminUIService;

    /**
     * Retrieves the editable Admin UI configuration.
     *
     * <p>On success the response contains an AppConfigResponse; on failure it contains a GenericResponse describing the error.</p>
     *
     * @return a Response containing an AppConfigResponse when successful, or a GenericResponse with error details when not
     */
    @Operation(summary = "Get Admin UI editable configuration", description = "Get Admin UI editable configuration", operationId = "get-adminui-conf", tags = {
            "Admin UI - Configuration"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ADMINUI_CONF_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { ADMINUI_CONF_WRITE}) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AppConfigResponse.class, description = "Admin UI editable configuration"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(CONFIG)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_CONF_READ}, groupScopes = {ADMINUI_CONF_WRITE}, superScopes = {ADMINUI_CONF_READ})
    public Response getConf() {
        try {
            log.info("Get Admin UI editable configuration.");
            AppConfigResponse appConf = adminUIService.getAdminUIEditableConfiguration();
            log.info("Configuration received.");
            return Response.ok(appConf).build();
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

    /**
     * Update the Admin UI editable configuration.
     *
     * @param appConfigResponse the Admin UI configuration to persist; must be valid and not null
     * @return the persisted, updated AppConfigResponse representing the current editable configuration
     */
    @Operation(summary = "Edit Admin UI editable configuration", description = "Edit Admin UI editable configuration", operationId = "edit-adminui-conf", tags = {
            "Admin UI - Configuration"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            ADMINUI_CONF_WRITE}))
    @RequestBody(description = "Admin Config object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfigResponse.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AppConfigResponse.class, description = "Admin UI editable configuration"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(CONFIG)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_CONF_WRITE}, superScopes = {ADMINUI_CONF_WRITE})
    public Response editConf(@Valid @NotNull AppConfigResponse appConfigResponse) {
        try {
            log.info("Editing Admin UI editable configuration.");
            AppConfigResponse appConf = adminUIService.editAdminUIEditableConfiguration(appConfigResponse);
            log.info("Configuration edited");
            return Response.ok(appConf).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SAVE_ADMIUI_CONFIG_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_CONFIG_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieve all admin UI roles.
     *
     * @return Response with HTTP 200 and a JSON array of AdminRole on success; on error a GenericResponse with an appropriate HTTP status and error details.
     */
    @Operation(summary = "Get all admin ui roles", description = "Get all admin ui roles", operationId = "get-all-adminui-roles", tags = {
            "Admin UI - Role"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) })                    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_READ}, groupScopes = {SCOPE_ROLE_WRITE}, superScopes = {SCOPE_ROLE_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllRoles() {
        try {
            log.info("Get all Admin-UI roles.");
            List<AdminRole> roles = adminUIService.getAllRoles();
            log.info("Roles received from Auth Server.");
            return Response.ok(roles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Add a new Admin UI role.
     *
     * @param roleArg the AdminRole to create; must be non-null and valid
     * @return a Response containing the saved list of AdminRole objects with HTTP 200 on success; on failure a GenericResponse with an error message and the appropriate HTTP status
     */
    @Operation(summary = "Add admin ui role", description = "Add admin ui role", operationId = "add-adminui-role", tags = {
            "Admin UI - Role"}, security ={
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) })  
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_WRITE}, superScopes = {SCOPE_ROLE_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Adding Admin-UI role.");
            List<AdminRole> savedRoles = adminUIService.addRole(roleArg);
            log.info("Added Admin-UI role..");
            return Response.ok(savedRoles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Update an existing Admin UI role.
     *
     * @param roleArg the AdminRole to update; must contain the identifying name and desired changes
     * @return a list of AdminRole objects reflecting the saved/updated roles
     */
    @Operation(summary = "Edit admin ui role", description = "Edit admin ui role", operationId = "edit-adminui-role", tags = {
            "Admin UI - Role"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) }) 
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of  AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_WRITE}, superScopes = {SCOPE_ROLE_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response editRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Editing Admin-UI role.");
            List<AdminRole> savedRoles = adminUIService.editRole(roleArg);
            log.info("Edited Admin-UI role..");
            return Response.ok(savedRoles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Retrieve details for the specified Admin UI role.
     *
     * @param adminUIRole the name of the Admin UI role to retrieve
     * @return the AdminRole object containing the role's details, or a server error response if retrieval fails
     */
    @Operation(summary = "Get admin ui role details by role-name", description = "Get admin ui role details by role-name", operationId = "get-adminui-role", tags = {
            "Admin UI - Role"}, security ={
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) })    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(ROLES + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_READ}, groupScopes = {SCOPE_ROLE_WRITE}, superScopes = {SCOPE_ROLE_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getRole(@Parameter(description = "Admin UI role") @PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Get all Admin-UI roles.");
            AdminRole roleObj = adminUIService.getRoleObjByName(adminUIRole);
            log.info("Roles received from Auth Server.");
            return Response.ok(roleObj).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Deletes the admin UI role identified by the given role name.
     *
     * @param adminUIRole the name of the role to delete; must not be null
     * @return the list of remaining AdminRole objects after deletion
     */
    @Operation(summary = "Delete admin ui role by role-name", description = "Delete admin ui role by role-name", operationId = "delete-adminui-role", tags = {
            "Admin UI - Role"}, security ={
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_DELETE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_DELETE }) })    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @DELETE
    @Path(ROLES + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_DELETE}, superScopes = {SCOPE_ROLE_ADMIN, AppConstants.SCOPE_ADMINUI_DELETE})
    public Response deleteRole(@Parameter(description = "Admin UI role") @PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Deleting Admin-UI role.");
            List<AdminRole> roles = adminUIService.deleteRole(adminUIRole);
            log.info("Deleted Admin-UI role..");
            return Response.ok(roles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription()))
                    .build();
        }
    }

    /**
     * Retrieve all Admin UI permissions.
     *
     * @return a JAX-RS Response whose entity is a List of AdminPermission on success; on error the response contains a GenericResponse describing the failure
     */
    @Operation(summary = "Get all admin ui permissions", description = "Get all admin ui permissions", operationId = "get-all-adminui-permissions", tags = {
            "Admin UI - Permission"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) }) 
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_READ}, groupScopes = {SCOPE_PERMISSION_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllPermissions() {
        try {
            log.info("Get all Admin-UI permissions.");
            List<AdminPermission> permissions = adminUIService.getPermissions();
            log.info("Permissions received from Auth Server.");
            return Response.ok(permissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Adds a new admin UI permission and returns the updated collection of permissions.
     *
     * @param permissionArg the AdminPermission to add; must be non-null and valid
     * @return the updated list of AdminPermission objects after the addition
     */
    @Operation(summary = "Add admin ui permissions", description = "Add admin ui permissions", operationId = "add-adminui-permission", tags = {
            "Admin UI - Permission"}, security =  {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) }) 
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addPermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Adding Admin-UI permissions.");
            List<AdminPermission> savedPermissions = adminUIService.addPermission(permissionArg);
            log.info("Added Admin-UI permissions..");
            return Response.ok(savedPermissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Update an admin UI permission and return the resulting list of permissions.
     *
     * @param permissionArg the AdminPermission to update
     * @return a list of updated AdminPermission objects
     */
    @Operation(summary = "Edit admin ui permissions", description = "Edit admin ui permissions", operationId = "edit-adminui-permission", tags = {
            "Admin UI - Permission"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) }) 
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response editPermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Editing Admin-UI permissions.");
            List<AdminPermission> savedPermissions = adminUIService.editPermission(permissionArg);
            log.info("Edited Admin-UI permissions..");
            return Response.ok(savedPermissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Retrieves an Admin UI permission by its name.
     *
     * @param adminUIPermission the permission name to look up
     * @return the AdminPermission matching the provided name
     */
    @Operation(summary = "Get admin ui permission by permission-name", description = "Get admin ui permission by permission-name", operationId = "get-adminui-permission", tags = {
            "Admin UI - Permission"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) }) 
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(PERMISSIONS + PERMISSION_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_READ}, groupScopes = {SCOPE_PERMISSION_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getPermission(@Parameter(description = "Admin UI Permission") @PathParam(PERMISSION_CONST) @NotNull String adminUIPermission) {
        try {
            log.info("Get Admin-UI permission.");
            AdminPermission permissionObj = adminUIService.getPermissionObjByName(adminUIPermission);
            log.info("Permission received from Auth Server.");
            return Response.ok(permissionObj).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Deletes the specified admin UI permission and returns the remaining permissions.
     *
     * @param permissionArg an AdminPermission object whose permission name will be deleted; must not be null
     * @return a Response whose entity is the list of remaining AdminPermission objects
     */
    @Operation(summary = "Delete admin ui permission", description = "Delete admin ui permission", operationId = "delete-adminui-permission", tags = {
            "Admin UI - Permission"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_DELETE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_DELETE }) }) 
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @DELETE
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_DELETE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_DELETE})
    public Response deletePermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            
            String adminUIPermission = null;
            if(permissionArg!=null) {
                adminUIPermission = permissionArg.getPermission();
            }
            log.info("Deleting Admin-UI permission.");
            List<AdminPermission> permissions = adminUIService.deletePermission(adminUIPermission);
            log.info("Deleted Admin-UI permission..");
            return Response.ok(permissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Retrieve all Admin UI role-to-permissions mappings.
     *
     * @return Response containing a List<RolePermissionMapping> on success, or a GenericResponse with error details on failure.
     */
    @Operation(summary = "Get all admin ui role-permissions mapping", description = "Get all admin ui role-permissions mapping", operationId = "get-all-adminui-role-permissions", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) }) 
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_READ}, groupScopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllAdminUIRolePermissionsMapping() {
        try {
            log.info("Get all Admin-UI role-permissions mapping.");
            List<RolePermissionMapping> roleScopeMapping = adminUIService.getAllAdminUIRolePermissionsMapping();
            log.info("Role-Permissions mapping received from Auth Server.");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription()))
                    .build();
        }
    }

    /**
     * Add role-to-permissions mappings for an admin UI role.
     *
     * @param rolePermissionMappingArg the RolePermissionMapping containing the role name and permissions to add; must not be null
     * @return a Response whose entity is a list of RolePermissionMapping on success, or a GenericResponse containing error details on failure
     */
    @Operation(summary = "Add role-permissions mapping", description = "Add role-permissions mapping", operationId = "add-role-permissions-mapping", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) })
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @POST
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Adding role-permissions to Admin-UI.");
            List<RolePermissionMapping> roleScopeMapping = adminUIService.addPermissionsToRole(rolePermissionMappingArg);
            log.info("Added role-permissions to Admin-UI..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription()))
                    .build();
        }
    }

    /**
     * Map the given permissions onto the specified role and return the updated mappings.
     *
     * @param rolePermissionMappingArg mapping containing the role identifier and the permissions to assign; must include the target role and one or more permissions
     * @return a list of RolePermissionMapping objects representing the role's permission mappings after the update
     */
    @Operation(summary = "Map permissions to role", description = "Map permissions to role", operationId = "map-permissions-to-role", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_WRITE }) })
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @PUT
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_WRITE})
    public Response mapPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Mapping permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = adminUIService.mapPermissionsToRole(rolePermissionMappingArg);
            log.info("Mapped permissions to Admin-UI role..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription()))
                    .build();
        }
    }

    /**
     * Retrieve the role-permissions mapping for the specified admin UI role.
     *
     * @param adminUIRole the name of the admin UI role to look up
     * @return a JAX-RS Response containing the RolePermissionMapping for the given role on success;
     *         on error, a GenericResponse with an error message and an appropriate HTTP status code
     */
    @Operation(summary = "Get admin ui role-permissions mapping by role-name", description = "Get admin ui role-permissions mapping by role-name", operationId = "get-adminui-role-permissions", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_READ }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_WRITE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_READ }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @GET
    @Path(ROLE_PERMISSIONS_MAPPING + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_READ}, groupScopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_READ})
    public Response getAdminUIRolePermissionsMapping(@Parameter(description = "Admin UI Role") @PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Get Admin-UI role-permissions mapping by role-name.");
            RolePermissionMapping roleScopeMapping = adminUIService.getAdminUIRolePermissionsMapping(adminUIRole);
            log.info("Role-Permissions mapping received from Auth Server.");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription()))
                    .build();
        }
    }

    /**
     * Removes all permission mappings associated with the specified Admin UI role.
     *
     * @param role the name of the Admin UI role whose permission mappings will be removed; must not be null
     * @return the list of RolePermissionMapping remaining after the removal
     */
    @Operation(summary = "Remove role-permissions mapping by role-name", description = "Remove role-permissions mapping by role-name", operationId = "remove-role-permissions-permission", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_ROLE_PERMISSION_MAPPING_DELETE }),
                    @SecurityRequirement(name = "oauth2", scopes = { SCOPE_PERMISSION_ADMIN }),
                    @SecurityRequirement(name = "oauth2", scopes = { AppConstants.SCOPE_ADMINUI_DELETE }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "Bad Request"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GenericResponse.class, description = "InternalServerError")))})
    @DELETE
    @Path(ROLE_PERMISSIONS_MAPPING + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_DELETE}, superScopes = {SCOPE_PERMISSION_ADMIN, AppConstants.SCOPE_ADMINUI_DELETE})
    public Response removePermissionsFromRole(@Parameter(description = "role") @PathParam(ROLE_CONST) @NotNull String role) {
        try {
            log.info("Removing permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = adminUIService.removePermissionsFromRole(role);
            log.info("Removed permissions to Admin-UI role..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription()))
                    .build();

        }
    }
}