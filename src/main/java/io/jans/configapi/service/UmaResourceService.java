/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class UmaResourceService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("resources");
        branch.setDn(getDnForResource(null));

        persistenceEntryManager.persist(branch);
    }

    public List<UmaResource> findResources(String pattern, int sizeLimit) {
        String[] targetArray = new String[]{pattern};
        Filter jsIdFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);

        return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, jsIdFilter, sizeLimit);
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
