package org.gluu.configapi.service;

import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;

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
        Filter oxIdFilter = Filter.createSubstringFilter("oxId", null, targetArray, null);

        return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, oxIdFilter, sizeLimit);
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
