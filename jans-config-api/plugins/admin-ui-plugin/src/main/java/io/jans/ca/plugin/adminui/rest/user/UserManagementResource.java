package io.jans.ca.plugin.adminui.rest.user;

import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.user.UserManagementService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;
import io.swagger.v3.oas.annotations.Operation;
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
public class UserManagementResource {

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

    @Inject
    Logger log;

    @Inject
    UserManagementService userManagementService;

    @Operation(summary = "Get all admin ui roles", description = "Get all admin ui roles", operationId = "get-all-adminui-roles", tags = {
            "Admin UI - Role"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_READ}, groupScopes = {SCOPE_ROLE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllRoles() {
        try {
            log.info("Get all Admin-UI roles.");
            List<AdminRole> roles = userManagementService.getAllRoles();
            log.info("Roles received from Auth Server.");
            return Response.ok(roles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Add admin ui role", description = "Add admin ui role", operationId = "add-adminui-role", tags = {
            "Admin UI - Role"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_WRITE}))
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @POST
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Adding Admin-UI role.");
            List<AdminRole> savedRoles = userManagementService.addRole(roleArg);
            log.info("Added Admin-UI role..");
            return Response.ok(savedRoles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Edit admin ui role", description = "Edit admin ui role", operationId = "edit-adminui-role", tags = {
            "Admin UI - Role"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_WRITE}))
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of  AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @PUT
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response editRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Editing Admin-UI role.");
            List<AdminRole> savedRoles = userManagementService.editRole(roleArg);
            log.info("Edited Admin-UI role..");
            return Response.ok(savedRoles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Get admin ui role details by role-name", description = "Get admin ui role details by role-name", operationId = "get-adminui-role", tags = {
            "Admin UI - Role"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(ROLES + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_READ}, groupScopes = {SCOPE_ROLE_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getRole(@PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Get all Admin-UI roles.");
            AdminRole roleObj = userManagementService.getRoleObjByName(adminUIRole);
            log.info("Roles received from Auth Server.");
            return Response.ok(roleObj).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Delete admin ui role by role-name", description = "Delete admin ui role by role-name", operationId = "delete-adminui-role", tags = {
            "Admin UI - Role"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_DELETE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @DELETE
    @Path(ROLES + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_DELETE}, superScopes = {AppConstants.SCOPE_ADMINUI_DELETE})
    public Response deleteRole(@PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Deleting Admin-UI role.");
            List<AdminRole> roles = userManagementService.deleteRole(adminUIRole);
            log.info("Deleted Admin-UI role..");
            return Response.ok(roles).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Get all admin ui permissions", description = "Get all admin ui permissions", operationId = "get-all-adminui-permissions", tags = {
            "Admin UI - Permission"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_PERMISSION_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_READ}, groupScopes = {SCOPE_PERMISSION_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllPermissions() {
        try {
            log.info("Get all Admin-UI permissions.");
            List<AdminPermission> permissions = userManagementService.getPermissions();
            log.info("Permissions received from Auth Server.");
            return Response.ok(permissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Add admin ui permissions", description = "Add admin ui permissions", operationId = "add-adminui-permission", tags = {
            "Admin UI - Permission"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_PERMISSION_WRITE}))
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @POST
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addPermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Adding Admin-UI permissions.");
            List<AdminPermission> savedPermissions = userManagementService.addPermission(permissionArg);
            log.info("Added Admin-UI permissions..");
            return Response.ok(savedPermissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Edit admin ui permissions", description = "Edit admin ui permissions", operationId = "edit-adminui-permission", tags = {
            "Admin UI - Permission"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_PERMISSION_WRITE}))
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @PUT
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response editPermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Editing Admin-UI permissions.");
            List<AdminPermission> savedPermissions = userManagementService.editPermission(permissionArg);
            log.info("Edited Admin-UI permissions..");
            return Response.ok(savedPermissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Get admin ui permission by permission-name", description = "Get admin ui permission by permission-name", operationId = "get-adminui-permission", tags = {
            "Admin UI - Permission"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_PERMISSION_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(PERMISSIONS + PERMISSION_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_READ}, groupScopes = {SCOPE_PERMISSION_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getPermission(@PathParam(PERMISSION_CONST) @NotNull String adminUIPermission) {
        try {
            log.info("Get Admin-UI permission.");
            AdminPermission permissionObj = userManagementService.getPermissionObjByName(adminUIPermission);
            log.info("Permission received from Auth Server.");
            return Response.ok(permissionObj).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Delete admin ui permission by permission-name", description = "Delete admin ui permission by permission-name", operationId = "delete-adminui-permission", tags = {
            "Admin UI - Permission"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_PERMISSION_DELETE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @DELETE
    @Path(PERMISSIONS + PERMISSION_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_PERMISSION_DELETE}, superScopes = {AppConstants.SCOPE_ADMINUI_DELETE})
    public Response deletePermission(@PathParam(PERMISSION_CONST) @NotNull String adminUIPermission) {
        try {
            log.info("Deleting Admin-UI permission.");
            List<AdminPermission> permissions = userManagementService.deletePermission(adminUIPermission);
            log.info("Deleted Admin-UI permission..");
            return Response.ok(permissions).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Get all admin ui role-permissions mapping", description = "Get all admin ui role-permissions mapping", operationId = "get-all-adminui-role-permissions", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_PERMISSION_MAPPING_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_READ}, groupScopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getAllAdminUIRolePermissionsMapping() {
        try {
            log.info("Get all Admin-UI role-permissions mapping.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.getAllAdminUIRolePermissionsMapping();
            log.info("Role-Permissions mapping received from Auth Server.");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Add role-permissions mapping", description = "Add role-permissions mapping", operationId = "add-role-permissions-mapping", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_PERMISSION_MAPPING_WRITE}))
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @POST
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response addPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Adding role-permissions to Admin-UI.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.addPermissionsToRole(rolePermissionMappingArg);
            log.info("Added role-permissions to Admin-UI..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Map permissions to role", description = "Map permissions to role", operationId = "map-permissions-to-role", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_PERMISSION_MAPPING_WRITE}))
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @PUT
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_WRITE})
    public Response mapPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Mapping permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.mapPermissionsToRole(rolePermissionMappingArg);
            log.info("Mapped permissions to Admin-UI role..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Get admin ui role-permissions mapping by role-name", description = "Get admin ui role-permissions mapping by role-name", operationId = "get-adminui-role-permissions", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_PERMISSION_MAPPING_READ}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @GET
    @Path(ROLE_PERMISSIONS_MAPPING + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_READ}, groupScopes = {SCOPE_ROLE_PERMISSION_MAPPING_WRITE}, superScopes = {AppConstants.SCOPE_ADMINUI_READ})
    public Response getAdminUIRolePermissionsMapping(@PathParam(ROLE_CONST) @NotNull String adminUIRole) {
        try {
            log.info("Get Admin-UI role-permissions mapping by role-name.");
            RolePermissionMapping roleScopeMapping = userManagementService.getAdminUIRolePermissionsMapping(adminUIRole);
            log.info("Role-Permissions mapping received from Auth Server.");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Operation(summary = "Remove role-permissions mapping by role-name", description = "Remove role-permissions mapping by role-name", operationId = "remove-role-permissions-permission", tags = {
            "Admin UI - Role-Permissions Mapping"}, security = @SecurityRequirement(name = "oauth2", scopes = {
            SCOPE_ROLE_PERMISSION_MAPPING_DELETE}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError")})
    @DELETE
    @Path(ROLE_PERMISSIONS_MAPPING + ROLE_PATH_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_ROLE_PERMISSION_MAPPING_DELETE}, superScopes = {AppConstants.SCOPE_ADMINUI_DELETE})
    public Response removePermissionsFromRole(@PathParam(ROLE_CONST) @NotNull String role) {
        try {
            log.info("Removing permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.removePermissionsFromRole(role);
            log.info("Removed permissions to Admin-UI role..");
            return Response.ok(roleScopeMapping).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}