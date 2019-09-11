/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.service.CacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides operations with resource set descriptions
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10.05.2012
 */
@Stateless
@Named
public class UmaResourceService {

    private static final int RESOURCE_CACHE_EXPIRATION_IN_SECONDS = 120;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private CacheService cacheService;

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("resources");
        branch.setDn(getDnForResource(null));

        ldapEntryManager.persist(branch);
    }

    /**
     * Add new resource description entry
     *
     * @param resource resource
     */
    public void addResource(UmaResource resource) {
        validate(resource);
        ldapEntryManager.persist(resource);
    }

    public void validate(UmaResource resource) {
        Preconditions.checkArgument(StringUtils.isNotBlank(resource.getName()), "Name is required for resource.");
        Preconditions.checkArgument(((resource.getScopes() != null && !resource.getScopes().isEmpty()) || StringUtils.isNotBlank(resource.getScopeExpression())), "Scope must be specified for resource.");
        Preconditions.checkState(!resource.isExpired(), "UMA Resource expired. It must not be expired.");
        prepareBranch();
    }

    public void updateResource(UmaResource resource) {
        updateResource(resource, false);
    }

    /**
     * Update resource description entry
     *
     * @param resource resource
     */
    public void updateResource(UmaResource resource, boolean skipValidation) {
        if (!skipValidation) {
            validate(resource);
        }
        ldapEntryManager.merge(resource);
    }

    /**
     * Remove resource description entry
     *
     * @param resource resource
     */
    public void remove(UmaResource resource) {
        ldapEntryManager.remove(resource);
    }

    /**
     * Remove resource description entry by ID.
     *
     * @param rsid resource ID
     */
    public void remove(String rsid) {
        ldapEntryManager.remove(getResourceById(rsid));
    }

    public void remove(List<UmaResource> resources) {
        for (UmaResource resource : resources) {
            remove(resource);
        }
    }

    /**
     * Get all resource descriptions
     *
     * @return List of resource descriptions
     */
    public List<UmaResource> getResourcesByAssociatedClient(String associatedClientDn) {
        try {
            prepareBranch();

            if (StringUtils.isNotBlank(associatedClientDn)) {
                final Filter filter = Filter.createEqualityFilter("oxAssociatedClient", associatedClientDn);
                return ldapEntryManager.findEntries(getBaseDnForResource(), UmaResource.class, filter);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public Set<UmaResource> getResources(Set<String> ids) {
        Set<UmaResource> result = new HashSet<UmaResource>();
        if (ids != null) {
            for (String id : ids) {
                UmaResource resource = getResourceById(id);
                if (resource != null) {
                    result.add(resource);
                } else {
                    log.error("Failed to find resource by id: " + id);
                }
            }
        }
        return result;
    }

    public UmaResource getResourceById(String id) {
        prepareBranch();

        try {
            final String key = getDnForResource(id);
            final UmaResource resource = cacheService.getWithPut(key, () -> ldapEntryManager.find(UmaResource.class, key), RESOURCE_CACHE_EXPIRATION_IN_SECONDS);
            if (resource != null) {
                return resource;
            }
        } catch (Exception e) {
            log.error("Failed to find resource set with id: " + id, e);
        }
        log.error("Failed to find resource set with id: " + id);
        throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_FOUND, UmaErrorResponseType.NOT_FOUND, "Failed to find resource set with id: " + id);
    }

    private void prepareBranch() {
        // Create resource description branch if needed
        if (!ldapEntryManager.contains(getDnForResource(null), SimpleBranch.class)) {
            addBranch();
        }
    }

    /**
     * Build DN string for resource description
     */
    public String getDnForResource(String oxId) {
        if (StringHelper.isEmpty(oxId)) {
            return getBaseDnForResource();
        }
        return String.format("oxId=%s,%s", oxId, getBaseDnForResource());
    }

    public String getBaseDnForResource() {
        final String umaBaseDn = staticConfiguration.getBaseDn().getUmaBase(); // "ou=uma,o=gluu"
        return String.format("ou=resources,%s", umaBaseDn);
    }
}
