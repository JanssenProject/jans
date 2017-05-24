/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import com.google.common.base.Preconditions;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.util.StringHelper;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

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

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private StaticConfiguration staticConfiguration;

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("uma_resource");
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
        Preconditions.checkArgument(resource.getScopes() != null && !resource.getScopes().isEmpty(), "Scope must be specified for resource.");
        prepareResourcesBranch();
    }

    /**
     * Update resource description entry
     *
     * @param resource resource
     */
    public void updateResource(UmaResource resource) {
        validate(resource);
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
    public List<UmaResource> getAllResources(String... ldapReturnAttributes) {
        return ldapEntryManager.findEntries(getBaseDnForResource(), UmaResource.class, ldapReturnAttributes, null);
    }

    /**
     * Get all resource descriptions
     *
     * @return List of resource descriptions
     */
    public List<UmaResource> getResourcesByAssociatedClient(String associatedClientDn) {
        try {
            prepareResourcesBranch();

            if (StringUtils.isNotBlank(associatedClientDn)) {
                final Filter filter = Filter.create(String.format("&(oxAssociatedClient=%s)", associatedClientDn));
                return ldapEntryManager.findEntries(getBaseDnForResource(), UmaResource.class, filter);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get resource descriptions by example
     *
     * @param resource Resource
     * @return Resource which conform example
     */
    public List<UmaResource> findResources(UmaResource resource) {
        return ldapEntryManager.findEntries(resource);
    }

    public boolean containsBranch() {
        return ldapEntryManager.contains(SimpleBranch.class, getDnForResource(null));
    }

    /**
     * Check if LDAP server contains resource description with specified attributes
     *
     * @return True if resource description with specified attributes exist
     */
    public boolean containsResource(UmaResource resource) {
        return ldapEntryManager.contains(resource);
    }

    public UmaResource getResourceById(String id) {

        prepareResourcesBranch();

        UmaResource ldapResource = new UmaResource();
        ldapResource.setDn(getBaseDnForResource());
        ldapResource.setId(id);

        final List<UmaResource> result = findResources(ldapResource);
        if (result.size() == 0) {
            log.error("Failed to find resource set with id: " + id);
            errorResponseFactory.throwUmaNotFoundException();
        } else if (result.size() > 1) {
            log.error("Multiple resource sets found with given id: " + id);
            errorResponseFactory.throwUmaInternalErrorException();
        }
        return result.get(0);
    }

    private void prepareResourcesBranch() {
        // Create resource description branch if needed
        if (!containsBranch()) {
            addBranch();
        }
    }

    /**
     * Get resource description by DN
     *
     * @param dn Resource description DN
     * @return Resource description
     */
    public UmaResource getResourceByDn(String dn) {
        return ldapEntryManager.find(UmaResource.class, dn);
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
        final String umaBaseDn = staticConfiguration.getBaseDn().getUmaBase(); // "ou=uma,o=@!1111,o=gluu"
        return String.format("ou=resources,%s", umaBaseDn);
    }

}
