/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service.uma;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxtrust.service.OrganizationService;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

/**
 * Provides operations with scope descriptions
 * 
 * @author Yuriy Movchan Date: 12/07/2012
 */
@ApplicationScoped
public class UmaScopeService implements Serializable {

	private static final long serialVersionUID = -3537567020929600777L;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;
	@Inject
	private OrganizationService organizationService;

	@Inject
	private Logger log;

	public void addBranch() {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("scopes");
		branch.setDn(getDnForScope(null));
		persistenceEntryManager.persist(branch);
	}

	public boolean containsBranch() {
		return persistenceEntryManager.contains(getDnForScope(null), SimpleBranch.class);
	}

	public void prepareScopeDescriptionBranch() {
		if (!containsBranch()) {
			addBranch();
		}
	}

	public Scope getUmaScopeByDn(String dn) {
		return persistenceEntryManager.find(Scope.class, dn);
	}

	public void addUmaScope(Scope scope) {
		persistenceEntryManager.persist(scope);
	}

	public void updateUmaScope(Scope scope) {
		persistenceEntryManager.merge(scope);
	}

	/**
	 * Remove scope description entry
	 * 
	 * @param scopeDescription
	 *            Scope description
	 */
	public void removeUmaScope(Scope scope) {
		persistenceEntryManager.remove(scope);
	}

	/**
	 * Check if LDAP server contains scope description with specified attributes
	 * 
	 * @return True if scope description with specified attributes exist
	 */
	public boolean containsUmaScope(String dn) {
		return persistenceEntryManager.contains(dn, Scope.class);
	}

	/**
	 * Get all scope descriptions
	 * 
	 * @return List of scope descriptions
	 */
	public List<Scope> getAllUmaScope(String... ldapReturnAttributes) {
		Filter scopeTypeFilter = Filter.createEqualityFilter("oxScopeType", "uma");
		List<Scope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, scopeTypeFilter,
				ldapReturnAttributes);
		return scopes;
	}

	/**
	 * Search scope descriptions by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of scope descriptions
	 */
	public List<Scope> findUmaScopes(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter oxIdFilter = Filter.createSubstringFilter("oxId", null, targetArray, null);
		Filter scopeTypeFilter = Filter.createEqualityFilter("oxScopeType", "uma");
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(oxIdFilter, displayNameFilter);
		List<Scope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), Scope.class,
				Filter.createANDFilter(searchFilter, scopeTypeFilter), sizeLimit);
		return scopes;
	}

	public List<Scope> getAllUmaScopes(int sizeLimit) {
		Filter scopeTypeFilter = Filter.createEqualityFilter("oxScopeType", "uma");
		List<Scope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, scopeTypeFilter,
				sizeLimit);
		return scopes;
	}

	/**
	 * Get scope descriptions by example
	 * 
	 * @param scopeDescription
	 *            Scope description
	 * @return List of ScopeDescription which conform example
	 */
	public List<Scope> findScope(Scope scopeDescription) {
		List<Scope> scopes = persistenceEntryManager.findEntries(scopeDescription);
		return scopes;
	}

	/**
	 * Get scope descriptions by Id
	 * 
	 * @param id
	 *            Id
	 * @return List of ScopeDescription which specified id
	 */
	public List<Scope> findUmaScopeById(String id) {
		return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class,
				Filter.createEqualityFilter("oxId", id));
	}

	/**
	 * Generate new inum for scope description
	 * 
	 * @return New inum for scope description
	 */
	public String generateInumForNewScope() {
		String newDn = null;
		String newInum = null;
		do {
			newInum = generateInumForNewScopeImpl();
			newDn = getDnForScope(newInum);
		} while (containsUmaScope(newDn));

		return newInum;
	}

	private String generateInumForNewScopeImpl() {
		return UUID.randomUUID().toString();
	}

	public String getDnForScope(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=scopes,%s", orgDn);
		}
		return String.format("inum=%s,ou=scopes,%s", inum, orgDn);
	}

	public Scope getUmaScopeByInum(String inum) {
		Scope umaScope = null;
		try {
			umaScope = persistenceEntryManager.find(Scope.class, getDnForScope(inum));
		} catch (Exception e) {
			log.error("Failed to find scope by Inum " + inum, e);
		}

		return umaScope;
	}

	public Scope getScopeByDn(String Dn) {
		try {
			return persistenceEntryManager.find(Scope.class, Dn);
		} catch (Exception e) {
			log.warn("", e);
			return null;
		}
	}

}
