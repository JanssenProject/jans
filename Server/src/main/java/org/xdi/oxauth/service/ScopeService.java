/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.service.CacheService;
import org.xdi.util.StringHelper;

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
    public List<org.xdi.oxauth.model.common.Scope> getAllScopesList() {
        String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();

        return ldapEntryManager.findEntries(scopesBaseDN,
                org.xdi.oxauth.model.common.Scope.class,
                Filter.createPresenceFilter("inum"));
    }

    public List<String> getDefaultScopesDn() {
        List<String> defaultScopes = new ArrayList<String>();

        for (org.xdi.oxauth.model.common.Scope scope : getAllScopesList()) {
            if (scope.getIsDefault()) {
                defaultScopes.add(scope.getDn());
            }
        }

        return defaultScopes;
    }

    public List<String> getScopesDn(List<String> scopeNames) {
        List<String> scopes = new ArrayList<String>();

        for (String scopeName : scopeNames) {
            org.xdi.oxauth.model.common.Scope scope = getScopeByDisplayName(scopeName);
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
    public org.xdi.oxauth.model.common.Scope getScopeByDn(String dn) {
        org.xdi.oxauth.model.common.Scope scope = fromCacheByDn(dn);
        if (scope == null) {
        	scope = ldapEntryManager.find(org.xdi.oxauth.model.common.Scope.class, dn);
        	putInCache(scope);
        }
        
        return scope;
    }

    /**
     * returns Scope by Dn
     *
     * @return Scope
     */
    public org.xdi.oxauth.model.common.Scope getScopeByDnSilently(String dn) {
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
    public org.xdi.oxauth.model.common.Scope getScopeByDisplayName(String displayName) {
        org.xdi.oxauth.model.common.Scope scope = fromCacheByName(displayName);
        if (scope == null) {
	        String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();
	
	        org.xdi.oxauth.model.common.Scope scopeExample = new org.xdi.oxauth.model.common.Scope();
	        scopeExample.setDn(scopesBaseDN);
	        scopeExample.setDisplayName(displayName);
	
	        List<org.xdi.oxauth.model.common.Scope> scopes = ldapEntryManager.findEntries(scopeExample);
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
    public List<org.xdi.oxauth.model.common.Scope> getScopeByClaim(String claimDn) {
    	List<org.xdi.oxauth.model.common.Scope> scopes = fromCacheByClaimDn(claimDn);
    	if (scopes == null) {
	        Filter filter = Filter.createEqualityFilter("oxAuthClaim", claimDn);
	        
	    	String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();
	        scopes = ldapEntryManager.findEntries(scopesBaseDN, org.xdi.oxauth.model.common.Scope.class, filter);  
	
	        putInCache(claimDn, scopes);
    	}

        return scopes;
    }

	public List<org.xdi.oxauth.model.common.Scope> getScopesByClaim(List<org.xdi.oxauth.model.common.Scope> scopes, String claimDn) {
		List<org.xdi.oxauth.model.common.Scope> result = new ArrayList<org.xdi.oxauth.model.common.Scope>();
		for (org.xdi.oxauth.model.common.Scope scope : scopes) {
			List<String> claims = scope.getOxAuthClaims();
			if ((claims != null) && claims.contains(claimDn)) {
				result.add(scope);
			}
			
		}

		return result;
	}

    private void putInCache(org.xdi.oxauth.model.common.Scope scope) {
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
    private void putInCache(String claimDn, List<org.xdi.oxauth.model.common.Scope> scopes) {
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

    private org.xdi.oxauth.model.common.Scope fromCacheByDn(String dn) {
        try {
            String key = getScopeDnCacheKey(dn);
            return (org.xdi.oxauth.model.common.Scope) cacheService.get(CACHE_SCOPE_NAME, key);
        } catch (Exception ex) {
            log.error("Failed to get scope from cache, scopeDn: '{}'", dn, ex);
            return null;
        }
    }

    private org.xdi.oxauth.model.common.Scope fromCacheByName(String name) {
        try {
            String key = getScopeNameCacheKey(name);
            return (org.xdi.oxauth.model.common.Scope) cacheService.get(CACHE_SCOPE_NAME, key);
        } catch (Exception ex) {
            log.error("Failed to get scope from cache, name: '{}'", name, ex);
            return null;
        }
    }

    private List<org.xdi.oxauth.model.common.Scope> fromCacheByClaimDn(String claimDn) {
        try {
        	String key = getClaimDnCacheKey(claimDn);
            return (List<org.xdi.oxauth.model.common.Scope>) cacheService.get(CACHE_SCOPE_NAME, key);
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