/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

/**
 * Provides operations with Scopes
 * 
 * @author Reda Zerrad Date: 06.18.2012
 */
@ApplicationScoped
public class ScopeService implements Serializable {

	private static final long serialVersionUID = 65734145678106186L;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;
	@Inject
	private OrganizationService organizationService;

	@Inject
	private Logger logger;

	/**
	 * Add new scope entry
	 * 
	 * @param scope
	 *            scope
	 */
	public void addScope(Scope scope) throws Exception {
		persistenceEntryManager.persist(scope);
	}

	/**
	 * Remove scope entry
	 * 
	 * @param scope
	 *            scope
	 */
	public void removeScope(Scope scope) throws Exception {

		persistenceEntryManager.remove(scope);
	}

	/**
	 * Get scope by inum
	 * 
	 * @param inum
	 *            scope Inum
	 * @return scope
	 */
	public Scope getScopeByInum(String inum) {
		Scope result = null;
		try {
			result = persistenceEntryManager.find(Scope.class, getDnForScope(inum));
		} catch (Exception e) {
			logger.debug("", e);
		}
		return result;
	}

	/**
	 * Build DN string for scope
	 * 
	 * @param inum
	 *            scope Inum
	 * @return DN string for specified scope or DN for scopes branch if inum is null
	 * @throws Exception
	 */
	public String getDnForScope(String inum) throws Exception {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=scopes,%s", orgDn);
		}
		return String.format("inum=%s,ou=scopes,%s", inum, orgDn);
	}

	/**
	 * Update scope entry
	 * 
	 * @param scope
	 *            scope
	 */
	public void updateScope(Scope scope) throws Exception {
		persistenceEntryManager.merge(scope);
	}

	/**
	 * Generate new inum for scope
	 * 
	 * @return New inum for scope
	 */
	public String generateInumForNewScope() throws Exception {
		Scope scope = new Scope();
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewScopeImpl();
			newDn = getDnForScope(newInum);
			scope.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, Scope.class));
		return newInum;
	}

	/**
	 * Search scopes by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of scopes
	 * @throws Exception
	 */
	public List<Scope> searchScopes(String pattern, int sizeLimit) {
		Filter searchFilter = null;
		Filter scopeTypeFilter = Filter.createEqualityFilter("oxScopeType", "openid");
		if (StringHelper.isNotEmpty(pattern)) {
			String[] targetArray = new String[] { pattern };
			Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray,
					null);
			Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray,
					null);
			searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		}
		List<Scope> result = new ArrayList<>();
		try {
			result = persistenceEntryManager.findEntries(getDnForScope(null), Scope.class,
					Filter.createANDFilter(searchFilter, scopeTypeFilter), sizeLimit);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Generate new inum for Scope
	 * 
	 * @return New inum for Scope
	 * @throws Exception
	 */
	private String generateInumForNewScopeImpl() throws Exception {
		return UUID.randomUUID().toString();
	}

	public List<Scope> getAllScopesList(int size) {
		try {
			Filter scopeTypeFilter = Filter.createEqualityFilter("oxScopeType", "openid");
			List<Scope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, scopeTypeFilter,
					size);
			return scopes;
		} catch (Exception e) {
			logger.error("", e);
			return new ArrayList<>();
		}
	}

	/**
	 * returns oxAuthScope by Dn
	 * 
	 * @return oxAuthScope
	 */

	public Scope getScopeByDn(String Dn) throws Exception {
		return persistenceEntryManager.find(Scope.class, Dn);
	}

	/**
	 * Get all available scope types
	 * 
	 * @return Array of scope types
	 */
	public List<ScopeType> getScopeTypes() {
		List<ScopeType> scopeTypes = new ArrayList<ScopeType>(
				Arrays.asList(org.gluu.oxauth.model.common.ScopeType.values()));
		scopeTypes.remove(ScopeType.UMA);
		return scopeTypes;
	}

	/**
	 * Get scope by DisplayName
	 * 
	 * @param DisplayName
	 * @return scope
	 */
	public Scope getScopeByDisplayName(String DisplayName) throws Exception {
		Scope scope = new Scope();
		scope.setDisplayName(DisplayName);
		List<Scope> scopes = persistenceEntryManager.findEntries(scope);
		if ((scopes != null) && (scopes.size() > 0)) {
			return scopes.get(0);
		}
		return null;
	}
}
