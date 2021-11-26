package io.jans.configapi.plugin.adminui.service.userManagement;

import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.RolePermissionMapping;

import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.model.userManagement.UserManagementRequest;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class UserManagementService {
    private static final String CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    public List<String> getRoles() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            return adminConf.getDynamic().getRoles();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<String> addRoles(List<String> rolesArgs) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            List<String> roles = adminConf.getDynamic().getRoles();

            //remove duplicate roles
            Set<String> rolesSet = new LinkedHashSet<>(roles);
            rolesSet.addAll(rolesArgs);
            List<String> combinedRoles = new ArrayList<>(rolesSet);

            adminConf.getDynamic().setRoles(combinedRoles);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRoles();
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<String> deleteRole(String roleArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(roleArg))
                    .collect(Collectors.toList());

            if(!roleScopeMapping.isEmpty()){
                List<String> permissions = roleScopeMapping.stream().findAny().get().getPermissions();
                if(!permissions.isEmpty()) {
                    log.error(ErrorResponse.UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS.getDescription()+"Role: {}",roleArg);
                    throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS.getDescription());
                }
            }

            List<String> roles = adminConf.getDynamic().getRoles();
            roles.removeIf(ele -> ele.equals(roleArg));

            adminConf.getDynamic().setRoles(roles);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRoles();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<String> getPermissions() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            return adminConf.getDynamic().getPermissions();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<String> addPermissions(List<String> permissionArgs) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            List<String> permissions = adminConf.getDynamic().getPermissions();

            //remove duplicate permissions
            Set<String> permissionsSet = new LinkedHashSet<>(permissions);
            permissionsSet.addAll(permissionArgs);
            List<String> combinedPermissions = new ArrayList<>(permissionsSet);

            adminConf.getDynamic().setPermissions(combinedPermissions);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getPermissions();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<String> deletePermission(String permissionArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getPermissions().contains(permissionArg))
                    .collect(Collectors.toList());

            boolean anyPermissionMapped = roleScopeMapping.stream().anyMatch(ele -> ele.getPermissions().contains(permissionArg));
            if(anyPermissionMapped) {
                log.error(ErrorResponse.UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE.getDescription());
            }

            List<String> permissions = adminConf.getDynamic().getPermissions();
            permissions.removeIf(ele -> ele.equals(permissionArg));

            adminConf.getDynamic().setPermissions(permissions);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getPermissions();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<RolePermissionMapping> getAdminUIRolePermissionsMapping() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription());
        }
    }

    public List<RolePermissionMapping> mapPermissionsToRole(UserManagementRequest userManagementRequest) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            List<String> roles = adminConf.getDynamic().getRoles();
            List<String> permissions = adminConf.getDynamic().getPermissions();

            if(!roles.contains(userManagementRequest.getRole())) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
            }
            if(!permissions.containsAll(userManagementRequest.getPermissions())) {
                log.error(ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
            }

            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(userManagementRequest.getRole()))
                    .collect(Collectors.toList());

            if (roleScopeMapping == null || roleScopeMapping.isEmpty()) {
                RolePermissionMapping rolePermissionMapping = new RolePermissionMapping();
                rolePermissionMapping.setRole(userManagementRequest.getRole());

                roleScopeMapping = new ArrayList<>();
                roleScopeMapping.add(rolePermissionMapping);
            }

            List<String> mappedPermissions = roleScopeMapping.stream().findFirst().get().getPermissions();
            //remove duplicate permissions
            Set<String> scopesSet = new LinkedHashSet<>(mappedPermissions);
            scopesSet.addAll(userManagementRequest.getPermissions());
            List<String> combinedScopes = new ArrayList<>(scopesSet);

            adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(userManagementRequest.getRole()))
                    .collect(Collectors.toList()).stream().findFirst().get().setPermissions(combinedScopes);

            entryManager.merge(adminConf);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
        }
    }

    public List<RolePermissionMapping> removePermissionsFromRole(UserManagementRequest userManagementRequest) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(userManagementRequest.getRole()))
                    .collect(Collectors.toList());

            if (roleScopeMapping == null || roleScopeMapping.isEmpty()) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
            }

            List<String> permissions = roleScopeMapping.stream().findFirst().get().getPermissions();
            permissions.removeIf(ele -> userManagementRequest.getPermissions().contains(ele));

            adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(userManagementRequest.getRole()))
                    .collect(Collectors.toList()).stream().findFirst().get().setPermissions(permissions);

            entryManager.merge(adminConf);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
        }
    }
}
