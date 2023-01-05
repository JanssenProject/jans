package io.jans.ca.plugin.adminui.service.user;

import com.google.api.client.util.Lists;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class UserManagementService {

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    public List<AdminRole> getAllRoles() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            return adminConf.getDynamic().getRoles();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public AdminRole getRoleObjByName(String role) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<AdminRole> roles = adminConf.getDynamic().getRoles().stream().filter(ele -> ele.getRole().equals(role)).collect(Collectors.toList());
            if (roles.isEmpty()) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.NOT_FOUND.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
            }
            return roles.stream().findFirst().get();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminRole> addRole(AdminRole roleArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<AdminRole> roles = adminConf.getDynamic().getRoles();

            if (roles.contains(roleArg)) {
                return adminConf.getDynamic().getRoles();
            }
            roles.add(roleArg);
            adminConf.getDynamic().setRoles(roles);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRoles();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.SAVE_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminRole> editRole(AdminRole roleArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);

            List<AdminRole> roles = adminConf.getDynamic().getRoles();
            if (roles.stream().noneMatch(ele -> ele.equals(roleArg))) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
            }
            roles.removeIf(ele -> ele.equals(roleArg));
            roles.add(roleArg);

            adminConf.getDynamic().setRoles(roles);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRoles();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminRole> deleteRole(String role) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);

            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(role))
                    .collect(Collectors.toList());

            if (!roleScopeMapping.isEmpty()) {
                Optional<RolePermissionMapping> rolePermissionMappingOptional = roleScopeMapping.stream().findAny();
                List<String> permissions = Lists.newArrayList();
                if (rolePermissionMappingOptional.isPresent()) {
                    permissions = rolePermissionMappingOptional.get().getPermissions();
                }
                if (!permissions.isEmpty()) {
                    log.error(ErrorResponse.UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS.getDescription());
                    throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS.getDescription());
                }
            }

            List<AdminRole> roles = adminConf.getDynamic().getRoles();
            if (isFalse(getRoleObjByName(role).getDeletable())) {
                log.error(ErrorResponse.ROLE_MARKED_UNDELETABLE.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_MARKED_UNDELETABLE.getDescription());
            }

            roles.removeIf(ele -> ele.getRole().equals(role));

            adminConf.getDynamic().setRoles(roles);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRoles();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.DELETE_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminPermission> getPermissions() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            return adminConf.getDynamic().getPermissions();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public AdminPermission getPermissionObjByName(String permission) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<AdminPermission> permissions = adminConf.getDynamic().getPermissions().stream().filter(ele -> ele.getPermission().equals(permission)).collect(Collectors.toList());
            if (permissions.isEmpty()) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.NOT_FOUND.getStatusCode(), ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
            }
            return permissions.stream().findFirst().get();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminPermission> addPermission(AdminPermission permissionArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<AdminPermission> permissions = adminConf.getDynamic().getPermissions();

            if (permissions.contains(permissionArg)) {
                return adminConf.getDynamic().getPermissions();
            }
            permissions.add(permissionArg);
            adminConf.getDynamic().setPermissions(permissions);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getPermissions();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.SAVE_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<AdminPermission> editPermission(AdminPermission permissionArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);

            List<AdminPermission> permissions = adminConf.getDynamic().getPermissions();
            if (permissions.stream().noneMatch(ele -> ele.equals(permissionArg))) {
                log.error(ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
            }
            permissions.removeIf(ele -> ele.equals(permissionArg));
            permissions.add(permissionArg);

            adminConf.getDynamic().setPermissions(permissions);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getPermissions();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<AdminPermission> deletePermission(String permission) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);

            boolean anyPermissionMapped = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().anyMatch(ele -> ele.getPermissions().contains(permission));

            if (anyPermissionMapped) {
                log.error(ErrorResponse.UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE.getDescription());
            }

            List<AdminPermission> permissions = adminConf.getDynamic().getPermissions();
            permissions.removeIf(ele -> ele.getPermission().equals(permission));

            adminConf.getDynamic().setPermissions(permissions);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getPermissions();

        } catch (ApplicationException e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.DELETE_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<RolePermissionMapping> getAllAdminUIRolePermissionsMapping() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription());
        }
    }

    public List<RolePermissionMapping> addPermissionsToRole(RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<RolePermissionMapping> roleScopeMappingList = getRolePermMapByRole(adminConf, rolePermissionMappingArg);

            if (CollectionUtils.isNotEmpty(roleScopeMappingList)) {
                log.warn(ErrorResponse.ROLE_PERMISSION_MAPPING_PRESENT.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_PERMISSION_MAPPING_PRESENT.getDescription());
            }

            //create new RolePermissionMapping
            RolePermissionMapping rolePermissionMapping = new RolePermissionMapping();
            //add role to it
            rolePermissionMapping.setRole(rolePermissionMappingArg.getRole());
            //remove duplicate permissions
            Set<String> scopesSet = new LinkedHashSet<>(rolePermissionMappingArg.getPermissions());
            List<String> combinedScopes = new ArrayList<>(scopesSet);
            rolePermissionMapping.setPermissions(combinedScopes);
            //add permission
            roleScopeMappingList.add(rolePermissionMapping);
            adminConf.getDynamic().getRolePermissionMapping().addAll(roleScopeMappingList);

            entryManager.merge(adminConf);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
        }
    }

    public List<RolePermissionMapping> mapPermissionsToRole(RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<RolePermissionMapping> roleScopeMappingList = getRolePermMapByRole(adminConf, rolePermissionMappingArg);

            if (roleScopeMappingList == null || roleScopeMappingList.isEmpty()) {
                RolePermissionMapping rolePermissionMapping = new RolePermissionMapping();
                rolePermissionMapping.setRole(rolePermissionMappingArg.getRole());

                roleScopeMappingList = Lists.newArrayList();
                roleScopeMappingList.add(rolePermissionMapping);
            }

            //remove duplicate permissions
            Set<String> scopesSet = new LinkedHashSet<>(rolePermissionMappingArg.getPermissions());
            List<String> combinedScopes = new ArrayList<>(scopesSet);

            if (adminConf.getDynamic().getRolePermissionMapping()
                    .stream().anyMatch(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))) {
                adminConf.getDynamic().getRolePermissionMapping()
                        .stream().filter(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))
                        .collect(Collectors.toList())
                        .forEach(ele -> ele.setPermissions(combinedScopes));
            } else {
                roleScopeMappingList.forEach(ele -> ele.setPermissions(combinedScopes));
                adminConf.getDynamic().getRolePermissionMapping().addAll(roleScopeMappingList);
            }

            entryManager.merge(adminConf);
            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
        }
    }

    public RolePermissionMapping getAdminUIRolePermissionsMapping(String role) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(role))
                    .collect(Collectors.toList());

            if (roleScopeMapping.isEmpty()) {
                log.error(ErrorResponse.ROLE_PERMISSION_MAP_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.NOT_FOUND.getStatusCode(), ErrorResponse.ROLE_PERMISSION_MAP_NOT_FOUND.getDescription());
            }
            return roleScopeMapping.stream().findFirst().get();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_READING_ROLE_PERMISSION_MAP.getDescription());
        }
    }

    public List<RolePermissionMapping> removePermissionsFromRole(String role) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            if (isFalse(getRoleObjByName(role).getDeletable())) {
                log.error(ErrorResponse.ROLE_MARKED_UNDELETABLE.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_MARKED_UNDELETABLE.getDescription());
            }
            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> !ele.getRole().equalsIgnoreCase(role))
                    .collect(Collectors.toList());
            adminConf.getDynamic().setRolePermissionMapping(roleScopeMapping);
            entryManager.merge(adminConf);

            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription());
        }
    }

    private List<RolePermissionMapping> getRolePermMapByRole(AdminConf adminConf, RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        validateRolePermissionMapping(adminConf, rolePermissionMappingArg);

        return adminConf.getDynamic().getRolePermissionMapping()
                .stream().filter(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))
                .collect(Collectors.toList());
    }

    private void validateRolePermissionMapping(AdminConf adminConf, RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        List<AdminRole> roles = adminConf.getDynamic().getRoles();
        List<AdminPermission> permissions = adminConf.getDynamic().getPermissions();

        if (roles.stream().noneMatch(ele -> ele.getRole().equals(rolePermissionMappingArg.getRole()))) {
            log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
        }
        if (permissions.stream().noneMatch(ele -> rolePermissionMappingArg.getPermissions().contains(ele.getPermission()))) {
            log.error(ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
            throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.PERMISSION_NOT_FOUND.getDescription());
        }
    }

    private static boolean isFalse(Boolean bool) {
        if (bool == null) {
            return true;
        }
        return bool.booleanValue() ? false : true;
    }
}