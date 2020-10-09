/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service.uma;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.python.jline.internal.Log;

import io.jans.scim.service.OrganizationService;
import io.jans.scim.util.OxTrustConstants;

/**
 * Provides operations with resources
 * 
 * @author Yuriy Movchan Date: 12/06/2012
 */
@ApplicationScoped
public class ResourceSetService implements Serializable {

	private static final long serialVersionUID = -1537567020929600777L;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;
	@Inject
	private OrganizationService organizationService;

	public void addBranch() {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("resources");
		branch.setDn(getDnForResource(null));

		persistenceEntryManager.persist(branch);
	}

	public boolean containsBranch() {
		return persistenceEntryManager.contains(getDnForResource(null), SimpleBranch.class);
	}

	/**
	 * Create resource branch if needed
	 */
	public void prepareResourceBranch() {
		if (!containsBranch()) {
			addBranch();
		}
	}

	/**
	 * Add new resource entry
	 * 
	 * @param resource
	 *            Resource
	 */
	public void addResource(UmaResource resource) {
		persistenceEntryManager.persist(resource);
	}

	/**
	 * Update resource entry
	 * 
	 * @param resource
	 *            Resource
	 */
	public void updateResource(UmaResource resource) {
		persistenceEntryManager.merge(resource);
	}

	/**
	 * Remove resource entry
	 * 
	 * @param resource
	 *            Resource
	 */
	public void removeResource(UmaResource resource) {
		persistenceEntryManager.remove(resource);
	}

	/**
	 * Check if LDAP server contains resource with specified attributes
	 * 
	 * @return True if resource with specified attributes exist
	 */
	public boolean containsResource(String dn) {
		return persistenceEntryManager.contains(dn, UmaResource.class);
	}

	public List<UmaResource> getAllResources(int sizeLimit) {
		return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, null, sizeLimit);
	}

	/**
	 * Get all resources
	 * 
	 * @return List of resources
	 */
	public List<UmaResource> getAllResources(String... ldapReturnAttributes) {
		return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class, null,
				ldapReturnAttributes);
	}

	/**
	 * Search resources by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of resources
	 */
	public List<UmaResource> findResources(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter oxIdFilter = Filter.createSubstringFilter("oxId", null, targetArray, null);
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(oxIdFilter, displayNameFilter);

		List<UmaResource> result = persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class,
				searchFilter, sizeLimit);

		return result;
	}

	/**
	 * Get resources by example
	 * 
	 * @param resource
	 *            Resource
	 * @return List of Resources which conform example
	 */
	public List<UmaResource> findResourceSets(UmaResource resource) {
		return persistenceEntryManager.findEntries(resource);
	}

	/**
	 * Get resources by Id
	 * 
	 * @param id
	 *            Id
	 * @return List of Resources which specified id
	 */
	public List<UmaResource> findResourcesById(String id) {
		return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class,
				Filter.createEqualityFilter("oxId", id));
	}

	/**
	 * Get resource set by DN
	 * 
	 * @param dn
	 *            Resource set DN
	 * @return Resource set
	 */
	public UmaResource getResourceByDn(String dn) {
		try {
			return persistenceEntryManager.find(UmaResource.class, dn);
		} catch (Exception e) {
			Log.info("Error fetching resource", e);
			return null;
		}

	}

	/**
	 * Generate new inum for resource set
	 * 
	 * @return New inum for resource set
	 */
	public String generateInumForNewResource() {
		String newDn = null;
		String newInum = null;
		do {
			newInum = generateInumForNewResourceImpl();
			newDn = getDnForResource(newInum);
		} while (containsResource(newDn));

		return newInum;
	}

	/**
	 * Generate new inum for resource set
	 * 
	 * @return New inum for resource set
	 */
	private String generateInumForNewResourceImpl() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Build DN string for resource
	 */
	public String getDnForResource(String oxId) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(oxId)) {
			return String.format("ou=resources,ou=uma,%s", orgDn);
		}

		return String.format("oxId=%s,ou=resources,ou=uma,%s", oxId, orgDn);
	}

	/**
	 * Get resources by scope
	 * 
	 * @param id
	 *            Id
	 * @return List of Resources which specified scope
	 */
	public List<UmaResource> findResourcesByScope(String scopeId) {
		return persistenceEntryManager.findEntries(getDnForResource(null), UmaResource.class,
				Filter.createEqualityFilter("oxAuthUmaScope", scopeId));
	}

}
