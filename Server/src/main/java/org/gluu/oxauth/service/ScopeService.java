/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.service.CacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 * @author Yuriy Movchan Date: 2016/04/26
 */
@Stateless
@Named
public class ScopeService {

	private static final String CACHE_SCOPE_NAME = "ScopeCache";

    @Inject
    private Logger log;

	@Inject
	private CacheService cacheService;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    /**
     * returns a list of all scopes
     *
     * @return list of scopes
     */
    public List<org.gluu.oxauth.persistence.model.Scope> getAllScopesList() {
        String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();

        return ldapEntryManager.findEntries(scopesBaseDN,
                org.gluu.oxauth.persistence.model.Scope.class,
                Filter.createPresenceFilter("inum"));
    }

    public List<String> getDefaultScopesDn() {
        List<String> defaultScopes = new ArrayList<String>();

        for (org.gluu.oxauth.persistence.model.Scope scope : getAllScopesList()) {
            if (scope.getIsDefault()) {
                defaultScopes.add(scope.getDn());
            }
        }

        return defaultScopes;
    }

    public List<String> getScopesDn(List<String> scopeNames) {
        List<String> scopes = new ArrayList<String>();

        for (String scopeName : scopeNames) {
            org.gluu.oxauth.persistence.model.Scope scope = getScopeByDisplayName(scopeName);
            if (scope != null) {
                scopes.add(scope.getDn());
            }
        }

        return scopes;
    }

    /**
     * returns Scope by Dn
     *
     * @return Scope
     */
    public org.gluu.oxauth.persistence.model.Scope getScopeByDn(String dn) {
        org.gluu.oxauth.persistence.model.Scope scope = fromCacheByDn(dn);
        if (scope == null) {
        	scope = ldapEntryManager.find(org.gluu.oxauth.persistence.model.Scope.class, dn);
        	putInCache(scope);
        }
        
        return scope;
    }

    /**
     * returns Scope by Dn
     *
     * @return Scope
     */
    public org.gluu.oxauth.persistence.model.Scope getScopeByDnSilently(String dn) {
        try {
            return getScopeByDn(dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get scope by DisplayName
     *
     * @param DisplayName
     * @return scope
     */
    public org.gluu.oxauth.persistence.model.Scope getScopeByDisplayName(String displayName) {
        org.gluu.oxauth.persistence.model.Scope scope = fromCacheByName(displayName);
        if (scope == null) {
	        String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();
	
	        org.gluu.oxauth.persistence.model.Scope scopeExample = new org.gluu.oxauth.persistence.model.Scope();
	        scopeExample.setDn(scopesBaseDN);
	        scopeExample.setDisplayName(displayName);
	
	        List<org.gluu.oxauth.persistence.model.Scope> scopes = ldapEntryManager.findEntries(scopeExample);
	        if ((scopes != null) && (scopes.size() > 0)) {
	        	scope = scopes.get(0);
	        }
	        
	        putInCache(scope);
        }

        return scope;
    }    
    
    /**
     * Get scope by oxAuthClaims
     *
     * @param oxAuthClaim
     * @return List of scope
     */
    public List<org.gluu.oxauth.persistence.model.Scope> getScopeByClaim(String claimDn) {
    	List<org.gluu.oxauth.persistence.model.Scope> scopes = fromCacheByClaimDn(claimDn);
    	if (scopes == null) {
	        Filter filter = Filter.createEqualityFilter("oxAuthClaim", claimDn);
	        
	    	String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();
	        scopes = ldapEntryManager.findEntries(scopesBaseDN, org.gluu.oxauth.persistence.model.Scope.class, filter);  
	
	        putInCache(claimDn, scopes);
    	}

        return scopes;
    }

	public List<org.gluu.oxauth.persistence.model.Scope> getScopesByClaim(List<org.gluu.oxauth.persistence.model.Scope> scopes, String claimDn) {
		List<org.gluu.oxauth.persistence.model.Scope> result = new ArrayList<org.gluu.oxauth.persistence.model.Scope>();
		for (org.gluu.oxauth.persistence.model.Scope scope : scopes) {
			List<String> claims = scope.getOxAuthClaims();
			if ((claims != null) && claims.contains(claimDn)) {
				result.add(scope);
			}
			
		}

		return result;
	}

    private void putInCache(org.gluu.oxauth.persistence.model.Scope scope) {
    	if (scope == null) {
    		return;
    	}

    	try {
            cacheService.put(CACHE_SCOPE_NAME, getScopeNameCacheKey(scope.getDisplayName()), scope, Constants.SKIP_CACHE_PUT_FOR_NATIVE_PERSISTENCE);
            cacheService.put(CACHE_SCOPE_NAME, getScopeDnCacheKey(scope.getDn()), scope, Constants.SKIP_CACHE_PUT_FOR_NATIVE_PERSISTENCE);
        } catch (Exception ex) {
            log.error("Failed to put scope in cache, scope: '{}'", scope, ex);
        }
    }
    private void putInCache(String claimDn, List<org.gluu.oxauth.persistence.model.Scope> scopes) {
    	if (scopes == null) {
    		return;
    	}

    	try {
        	String key = getClaimDnCacheKey(claimDn);
            cacheService.put(CACHE_SCOPE_NAME, key, scopes);
        } catch (Exception ex) {
            log.error("Failed to put scopes in cache, claimDn: '{}'", claimDn, ex);
        }
    }

    private org.gluu.oxauth.persistence.model.Scope fromCacheByDn(String dn) {
        try {
            String key = getScopeDnCacheKey(dn);
            return (org.gluu.oxauth.persistence.model.Scope) cacheService.get(CACHE_SCOPE_NAME, key);
        } catch (Exception ex) {
            log.error("Failed to get scope from cache, scopeDn: '{}'", dn, ex);
            return null;
        }
    }

    private org.gluu.oxauth.persistence.model.Scope fromCacheByName(String name) {
        try {
            String key = getScopeNameCacheKey(name);
            return (org.gluu.oxauth.persistence.model.Scope) cacheService.get(CACHE_SCOPE_NAME, key);
        } catch (Exception ex) {
            log.error("Failed to get scope from cache, name: '{}'", name, ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
	private List<org.gluu.oxauth.persistence.model.Scope> fromCacheByClaimDn(String claimDn) {
        try {
        	String key = getClaimDnCacheKey(claimDn);
            return (List<org.gluu.oxauth.persistence.model.Scope>) cacheService.get(CACHE_SCOPE_NAME, key);
        } catch (Exception ex) {
            log.error("Failed to get scopes from cache, claimDn: '{}'", claimDn, ex);
            return null;
        }
    }

    private static String getClaimDnCacheKey(String claimDn) {
        return "claim_dn" + StringHelper.toLowerCase(claimDn);
    }

    private static String getScopeNameCacheKey(String name) {
        return "scope_name_" + StringHelper.toLowerCase(name);
    }

    private static String getScopeDnCacheKey(String dn) {
        return "scope_dn_" + StringHelper.toLowerCase(dn);
    }

}