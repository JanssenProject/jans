/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.core.service.ConfigUserService;
import io.jans.configapi.core.model.role.*;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class RolePermissionMappingService {
    private static final String ROLE_PERMISSION_MAPPING_CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";
    private static final String ERROR_READING_ROLE_PERMISSION_MAP = "Error in reading role-permissions mapping from Auth Server";
    private static final String ROLE_PERMISSION_MAP_NOT_FOUND = "Role-permissions mapping not found.";

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    ConfigUserService configUserService;

    public User getUserByInum(String inum) {
        User user = null;
        if (StringUtils.isBlank(inum)) {
            return user;
        }
        user = configUserService.getUserByInum(inum);
        if (user == null) {
            logger.error("User % not found. Cannot lock account.", inum);
            return user;
        }
        logger.error(" user:{}, user.getCustomAttributes():{}", user, user.getCustomAttributes());

        return user;

    }

    /**
     * Loads the RolePermissionMappingConf configuration entry from persistence.
     *
     * @return the RolePermissionMappingConf instance stored or {@code null} if not
     *         found
     */
    public RolePermissionMappingConf fetchRolePermissionMappingConf() {
        return persistenceEntryManager.find(RolePermissionMappingConf.class, ROLE_PERMISSION_MAPPING_CONFIG_DN);
    }

    public List<RolePermissionMapping> getAllRolePermissionMapping() {
        try {
            RolePermissionMappingConf rolePermissionMappingConf = persistenceEntryManager
                    .find(RolePermissionMappingConf.class, ROLE_PERMISSION_MAPPING_CONFIG_DN);
            return rolePermissionMappingConf.getDynamic().getRolePermissionMapping();
        } catch (Exception ex) {
            logger.error(ERROR_READING_ROLE_PERMISSION_MAP, ex);
            throw new WebApplicationException(ERROR_READING_ROLE_PERMISSION_MAP,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    public RolePermissionMapping getPermissionsMappingByRole(String role) {
        try {
            RolePermissionMappingConf rolePermissionMappingConf = persistenceEntryManager
                    .find(RolePermissionMappingConf.class, ROLE_PERMISSION_MAPPING_CONFIG_DN);
            List<RolePermissionMapping> roleScopeMappings = rolePermissionMappingConf.getDynamic()
                    .getRolePermissionMapping().stream().filter(ele -> ele.getRole().equalsIgnoreCase(role))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(roleScopeMappings)) {
                return roleScopeMappings.get(0);
            }
            logger.error(ROLE_PERMISSION_MAP_NOT_FOUND);
            throw new WebApplicationException(ROLE_PERMISSION_MAP_NOT_FOUND, Response.Status.NOT_FOUND.getStatusCode());

        } catch (Exception ex) {
            logger.error(ROLE_PERMISSION_MAP_NOT_FOUND, ex);
            throw new WebApplicationException(ROLE_PERMISSION_MAP_NOT_FOUND,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

}
