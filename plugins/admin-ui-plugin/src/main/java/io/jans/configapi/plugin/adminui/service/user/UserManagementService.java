package io.jans.configapi.plugin.adminui.service.user;

import com.google.api.client.util.Lists;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.AdminPermission;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class UserManagementService {
    private static final String CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    public List<AdminRole> getRoles() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            return adminConf.getDynamic().getRoles();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminRole> addRole(AdminRole roleArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
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
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

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
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.EDIT_ADMIUI_ROLES_ERROR.getDescription());
        }
    }

    public List<AdminRole> deleteRole(String role) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

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
            roles.removeIf(ele -> ele.getRole().equals(role));

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

    public List<AdminPermission> getPermissions() throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            return adminConf.getDynamic().getPermissions();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<AdminPermission> addPermission(AdminPermission permissionArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
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
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

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
            log.error(ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.EDIT_ADMIUI_PERMISSIONS_ERROR.getDescription());
        }
    }

    public List<AdminPermission> deletePermission(String permission) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);

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

    public List<RolePermissionMapping> mapPermissionsToRole(RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
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

            List<RolePermissionMapping> roleScopeMappingList = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))
                    .collect(Collectors.toList());

            if (roleScopeMappingList == null || roleScopeMappingList.isEmpty()) {
                RolePermissionMapping rolePermissionMapping = new RolePermissionMapping();
                rolePermissionMapping.setRole(rolePermissionMappingArg.getRole());

                roleScopeMappingList = Lists.newArrayList();
                roleScopeMappingList.add(rolePermissionMapping);
            }

            Optional<RolePermissionMapping> rolePermissionMappingOptional = roleScopeMappingList.stream().findFirst();
            List<String> mappedPermissions = Lists.newArrayList();
            if (rolePermissionMappingOptional.isPresent()) {
                mappedPermissions = rolePermissionMappingOptional.get().getPermissions();
            }

            //remove duplicate permissions
            Set<String> scopesSet = new LinkedHashSet<>(mappedPermissions);
            scopesSet.addAll(rolePermissionMappingArg.getPermissions());
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
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_MAPPING_ROLE_PERMISSION.getDescription());
        }
    }

    public List<RolePermissionMapping> removePermissionsFromRole(RolePermissionMapping rolePermissionMappingArg) throws ApplicationException {
        try {
            AdminConf adminConf = entryManager.find(AdminConf.class, CONFIG_DN);
            List<RolePermissionMapping> roleScopeMapping = adminConf.getDynamic().getRolePermissionMapping()
                    .stream().filter(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))
                    .collect(Collectors.toList());

            if (roleScopeMapping == null || roleScopeMapping.isEmpty()) {
                log.error(ErrorResponse.ROLE_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ROLE_NOT_FOUND.getDescription());
            }

            Optional<RolePermissionMapping> rolePermissionMappingOptional = roleScopeMapping.stream().findFirst();

            if (rolePermissionMappingOptional.isPresent()) {
                List<String> permissions = rolePermissionMappingOptional.get().getPermissions();
                permissions.removeIf(ele -> rolePermissionMappingArg.getPermissions().contains(ele));

                adminConf.getDynamic().getRolePermissionMapping()
                        .stream().filter(ele -> ele.getRole().equalsIgnoreCase(rolePermissionMappingArg.getRole()))
                        .collect(Collectors.toList()).forEach(ele -> ele.setPermissions(permissions));

                entryManager.merge(adminConf);
            }

            return adminConf.getDynamic().getRolePermissionMapping();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription(), e);
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_DELETING_ROLE_PERMISSION.getDescription());
        }
    }
}