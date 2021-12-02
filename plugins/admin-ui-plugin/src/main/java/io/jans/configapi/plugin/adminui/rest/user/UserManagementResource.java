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

    static final String GET_ADMINUI_ROLES = "/getRoles";
    static final String ADD_ADMINUI_ROLE = "/addRole";
    static final String EDIT_ADMINUI_ROLE = "/editRole";
    static final String DELETE_ADMINUI_ROLE = "/deleteRole";
    static final String GET_ADMINUI_PERMISSIONS = "/getPermissions";
    static final String ADD_ADMINUI_PERMISSION = "/addPermission";
    static final String EDIT_ADMINUI_PERMISSION = "/editPermission";
    static final String DELETE_ADMINUI_PERMISSION = "/deletePermission";
    static final String GET_ADMINUI_ROLE_PERMISSIONS_MAPPING = "/getRolePermissionsMapping";
    static final String MAP_PERMISSIONS_TO_ROLE = "/mapPermissionsToRole";
    static final String REMOVE_PERMISSIONS_FROM_ROLE = "/removePermissionsFromRole";
    static final String ADMINUI_PERMISSIONS_READ = "https://jans.io/adminui/user/permissions.read";
    static final String ADMINUI_PERMISSIONS_WRITE = "https://jans.io/adminui/user/permissions.write";

    @Inject
    Logger log;

    @Inject
    UserManagementService userManagementService;

    @GET
    @Path(GET_ADMINUI_ROLES)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_READ)
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
    @Path(ADD_ADMINUI_ROLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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
    @Path(EDIT_ADMINUI_ROLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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

    @DELETE
    @Path(DELETE_ADMINUI_ROLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
    public Response deleteRole(@Valid @NotNull AdminRole roleArg) {
        try {
            log.info("Deleting Admin-UI role.");
            List<AdminRole> roles = userManagementService.deleteRole(roleArg);
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
    @Path(GET_ADMINUI_PERMISSIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_READ)
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
    @Path(ADD_ADMINUI_PERMISSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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
    @Path(EDIT_ADMINUI_PERMISSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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

    @DELETE
    @Path(DELETE_ADMINUI_PERMISSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
    public Response deletePermission(@Valid @NotNull AdminPermission permissionArg) {
        try {
            log.info("Deleting Admin-UI permission.");
            List<AdminPermission> permissions = userManagementService.deletePermission(permissionArg);
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
    @Path(GET_ADMINUI_ROLE_PERMISSIONS_MAPPING)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_READ)
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
    @Path(MAP_PERMISSIONS_TO_ROLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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
    @Path(REMOVE_PERMISSIONS_FROM_ROLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = ADMINUI_PERMISSIONS_WRITE)
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

