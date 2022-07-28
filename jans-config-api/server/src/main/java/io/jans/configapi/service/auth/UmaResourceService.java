/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class UmaResourceService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private Logger logger;

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("resources");
        branch.setDn(getDnForResource(null));

        persistenceEntryManager.persist(branch);
    }

    public List<UmaResource> findResources(String pattern, int sizeLimit) {
        String[] targetArray = new String[] { pattern };
        Filter jsIdFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter searchFilter = Filter.createORFilter(jsIdFilter, displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, searchFilter, sizeLimit);
    }

    public List<UmaResource> findResourcesByName(String name, int sizeLimit) {

        if (StringUtils.isNotBlank(name)) {
            Filter searchFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, name);
            return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, searchFilter,
                    sizeLimit);
        }
        return Collections.emptyList();
    }

    public List<UmaResource> getAllResources(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, null, sizeLimit);
    }

    public void addResource(UmaResource resource) {
        persistenceEntryManager.persist(resource);
    }

    public void updateResource(UmaResource resource) {
        persistenceEntryManager.merge(resource);
    }

    public void remove(UmaResource resource) {
        persistenceEntryManager.remove(resource);
    }

    public void remove(String rsid) {
        persistenceEntryManager.remove(getResourceById(rsid));
    }

    public UmaResource getResourceById(String id) {
        prepareBranch();
        final String dn = getDnForResource(id);
        return persistenceEntryManager.find(UmaResource.class, dn);
    }

    public List<UmaResource> getResourcesByClient(String clientDn) {
        try {
            logger.debug(" Fetch UmaResource based on client - clientDn:{} ", clientDn);
            prepareBranch();

            if (StringUtils.isNotBlank(clientDn)) {
                return persistenceEntryManager.findEntries(getBaseDnForResource(), UmaResource.class,
                        Filter.createEqualityFilter("jansAssociatedClnt", clientDn));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private void prepareBranch() {
        if (!persistenceEntryManager.hasBranchesSupport(getDnForResource(null))) {
            return;
        }

        // Create resource description branch if needed
        if (!persistenceEntryManager.contains(getDnForResource(null), SimpleBranch.class)) {
            addBranch();
        }
    }

    public String getDnForResource(String jsId) {
        if (StringHelper.isEmpty(jsId)) {
            return getBaseDnForResource();
        }
        return String.format("jansId=%s,%s", jsId, getBaseDnForResource());
    }

    public String getBaseDnForResource() {
        final String umaBaseDn = staticConfiguration.getBaseDn().getUmaBase(); // "ou=uma,o=jans"
        return String.format("ou=resources,%s", umaBaseDn);
    }
}
