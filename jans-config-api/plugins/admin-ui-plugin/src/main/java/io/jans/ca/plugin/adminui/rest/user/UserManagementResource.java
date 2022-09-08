package io.jans.ca.plugin.adminui.rest.user;

import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.user.UserManagementService;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.List;

@Path("/admin-ui/user")
public class UserManagementResource {

    static final String ROLES = "/roles";
    static final String PERMISSIONS = "/permissions";
    static final String ROLE_PERMISSIONS_MAPPING = "/rolePermissionsMapping";
    static final String SCOPE_ROLE_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly";
    static final String SCOPE_ROLE_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write";
    static final String SCOPE_PERMISSION_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly";
    static final String SCOPE_PERMISSION_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly";
    static final String SCOPE_ROLE_PERMISSION_MAPPING_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write";

    @Inject
    Logger log;

    @Inject
    UserManagementService userManagementService;

    @Operation(summary = "Get all admin ui roles", description = "Get all admin ui roles", operationId = "get-adminui-roles", tags = {
            "Admin UI - Role" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_READ }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_READ)
    public Response getRoles() {
        try {
            log.info("Get all Admin-UI roles.");
            List<AdminRole> roles = userManagementService.getRoles();
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
            "Admin UI - Role" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_WRITE }))
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_WRITE)
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
            "Admin UI - Role" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_WRITE }))
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of  AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_WRITE)
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

    @Operation(summary = "Delete admin ui role", description = "Delete admin ui role", operationId = "delete-adminui-role", tags = {
            "Admin UI - Role" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_WRITE }))
    @RequestBody(description = "AdminRole object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminRole.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminRole.class, description = "List of AdminRole")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_WRITE)
    public Response deleteRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Deleting Admin-UI role.");
            List<AdminRole> roles = userManagementService.deleteRole(roleArg.getRole());
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

    @Operation(summary = "Get admin ui permissions", description = "Get admin ui permissions", operationId = "get-adminui-permissions", tags = {
            "Admin UI - Permission" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_PERMISSION_READ }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_PERMISSION_READ)
    public Response getPermissions() {
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
            "Admin UI - Permission" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_PERMISSION_WRITE }))
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_PERMISSION_WRITE)
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
            "Admin UI - Permission" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_PERMISSION_WRITE }))
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_PERMISSION_WRITE)
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

    @Operation(summary = "Delete admin ui permissions", description = "Delete admin ui permissions", operationId = "delete-adminui-permission", tags = {
            "Admin UI - Permission" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_PERMISSION_WRITE }))
    @RequestBody(description = "AdminPermission object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AdminPermission.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AdminPermission.class, description = "List of AdminPermission")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_PERMISSION_WRITE)
    public Response deletePermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Deleting Admin-UI permission.");
            List<AdminPermission> permissions = userManagementService.deletePermission(permissionArg.getPermission());
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

    @Operation(summary = "Get admin ui role-permissions mapping", description = "Get admin ui role-permissions mapping", operationId = "get-adminui-role-permissions", tags = {
            "Admin UI - Role-Permissions Mapping" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_PERMISSION_MAPPING_READ }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_PERMISSION_MAPPING_READ)
    public Response getAdminUIRolePermissionsMapping() {
        try {
            log.info("Get all Admin-UI role-permissions mapping.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.getAdminUIRolePermissionsMapping();
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
            "Admin UI - Role-Permissions Mapping" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_PERMISSION_MAPPING_WRITE}))
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_PERMISSION_MAPPING_WRITE)
    public Response addPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Adding role-permissions to Admin-UI.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService
                    .addPermissionsToRole(rolePermissionMappingArg);
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
            "Admin UI - Role-Permissions Mapping" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_PERMISSION_MAPPING_WRITE }))
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_PERMISSION_MAPPING_WRITE)
    public Response mapPermissionsToRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Mapping permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService
                    .mapPermissionsToRole(rolePermissionMappingArg);
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

    @Operation(summary = "Remove role-permissions mapping", description = "Remove role-permissions mapping", operationId = "remove-role-permissions-permission", tags = {
            "Admin UI - Role-Permissions Mapping" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    SCOPE_ROLE_PERMISSION_MAPPING_WRITE }))
    @RequestBody(description = "RolePermissionMapping object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RolePermissionMapping.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RolePermissionMapping.class, description = "List of RolePermissionMapping")))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = SCOPE_ROLE_PERMISSION_MAPPING_WRITE)
    public Response removePermissionsFromRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Removing permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService
                    .removePermissionsFromRole(rolePermissionMappingArg);
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