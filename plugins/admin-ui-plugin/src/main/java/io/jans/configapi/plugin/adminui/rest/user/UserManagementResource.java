package io.jans.configapi.plugin.adminui.rest.user;

import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.service.user.UserManagementService;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/admin-ui/user")
public class UserManagementResource {

    static final String ROLES = "/roles";
    static final String PERMISSIONS = "/permissions";
    static final String ROLE_PERMISSIONS_MAPPING = "/rolePermissionsMapping";
    static final String ADMINUI_ROLE_READ = "https://jans.io/adminui/user/role.read";
    static final String ADMINUI_ROLE_WRITE = "https://jans.io/adminui/user/role.write";
    static final String ADMINUI_PERMISSION_READ = "https://jans.io/adminui/user/permission.read";
    static final String ADMINUI_PERMISSION_WRITE = "https://jans.io/adminui/user/permission.write";
    static final String ADMINUI_ROLE_PERMISSION_MAPPING_READ = "https://jans.io/adminui/user/rolePermissionMapping.read";
    static final String ADMINUI_ROLE_PERMISSION_MAPPING_WRITE = "https://jans.io/adminui/user/rolePermissionMapping.write";

    @Inject
    Logger log;

    @Inject
    UserManagementService userManagementService;

    @GET
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_READ)
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

    @POST
    @Path(ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_WRITE)
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

    @PUT
    @Path(ROLES + "/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_WRITE)
    public Response editRole(@PathParam("role") String role, @Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Editing Admin-UI role.");
            roleArg.setRole(role);
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

    @DELETE
    @Path(ROLES + "/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_WRITE)
    public Response deleteRole(@PathParam("role") String role) {
        try {
            log.info("Deleting Admin-UI role.");
            List<AdminRole> roles = userManagementService.deleteRole(role);
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

    @GET
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSION_READ)
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

    @POST
    @Path(PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSION_WRITE)
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

    @PUT
    @Path(PERMISSIONS + "/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSION_WRITE)
    public Response editPermission(@PathParam("permission") String permission, @Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Editing Admin-UI permissions.");
            permissionArg.setPermission(permission);
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

    @DELETE
    @Path(PERMISSIONS + "/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSION_WRITE)
    public Response deletePermission(@PathParam("permission") String permission) {
        try {
            log.info("Deleting Admin-UI permission.");
            List<AdminPermission> permissions = userManagementService.deletePermission(permission);
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

    @GET
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_PERMISSION_MAPPING_READ)
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

    @PUT
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_PERMISSION_MAPPING_WRITE)
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

    @DELETE
    @Path(ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_ROLE_PERMISSION_MAPPING_WRITE)
    public Response removePermissionsFromRole(@Valid @NotNull RolePermissionMapping rolePermissionMappingArg) {
        try {
            log.info("Removing permissions to Admin-UI role.");
            List<RolePermissionMapping> roleScopeMapping = userManagementService.removePermissionsFromRole(rolePermissionMappingArg);
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