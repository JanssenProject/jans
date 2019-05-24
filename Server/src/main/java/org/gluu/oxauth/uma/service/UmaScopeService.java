/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.service.InumService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 22/04/2013
 */
@Stateless
@Named("umaScopeService")
public class UmaScopeService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private InumService inumService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    public Scope getScope(String scopeId) {
        try {
            final Filter filter = Filter.createEqualityFilter("oxId", scopeId);
            final List<Scope> entries = ldapEntryManager.findEntries(baseDn(), Scope.class, filter);
            if (entries != null && !entries.isEmpty()) {
                // if more then one scope then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one UMA scope, id: {}", scopeId);
                    for (Scope s : entries) {
                        log.error("Scope, Id: {}, dn: {}", s.getId(), s.getDn());
                    }
                }
                return entries.get(0);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean persist(Scope scope) {
        try {
            if (StringUtils.isBlank(scope.getDn())) {
                scope.setDn(String.format("inum=%s,%s", scope.getInum(), baseDn()));
            }

            ldapEntryManager.persist(scope);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<String> getScopeDNsByIdsAndAddToLdapIfNeeded(List<String> scopeIds) {
        List<String> result = new ArrayList<String>();
        for (Scope scope : getScopesByIds(scopeIds)) {
            result.add(scope.getDn());
        }
        return result;
    }

    public List<Scope> getScopesByDns(List<String> scopeDns) {
        final List<Scope> result = new ArrayList<Scope>();
        try {
            if (scopeDns != null && !scopeDns.isEmpty()) {
                for (String dn : scopeDns) {
                    final Scope scopeDescription = ldapEntryManager.find(Scope.class, dn);
                    if (scopeDescription != null) {
                        result.add(scopeDescription);
                    } else {
                        log.error("Failed to load UMA scope with dn: {}", dn);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public List<String> getScopeIdsByDns(List<String> scopeDns) {
        return getScopeIds(getScopesByDns(scopeDns));
    }

    public List<String> getScopeIds(List<Scope> scopes) {
        final List<String> result = new ArrayList<String>();
        if (scopes != null && !scopes.isEmpty()) {
            for (Scope scope : scopes) {
                result.add(scope.getId());
            }
        }
        return result;
    }

    public List<Scope> getScopesByIds(List<String> scopeIds) {
        List<Scope> result = new ArrayList<Scope>();
        if (scopeIds != null && !scopeIds.isEmpty()) {
            List<String> notInLdap = new ArrayList<String>(scopeIds);

            final List<Scope> entries = ldapEntryManager.findEntries(baseDn(), Scope.class, createAnyFilterByIds(scopeIds));
            if (entries != null) {
                result.addAll(entries);
                for (Scope scope : entries) {
                    notInLdap.remove(scope.getId());
                }
            }

            if (!notInLdap.isEmpty()) {
                for (String scopeId : notInLdap) {
                    result.add(addScope(scopeId));
                }
            }
        }
        return result;
    }

    private Scope addScope(String scopeId) {
        final Boolean addAutomatically = appConfiguration.getUmaAddScopesAutomatically();
        if (addAutomatically != null && addAutomatically) {
            final String inum = inumService.generateInum();
            final Scope newScope = new Scope();
            newScope.setScopeType(ScopeType.UMA);
            newScope.setInum(inum);
            newScope.setDisplayName(scopeId);
            newScope.setId(scopeId);

            final boolean persisted = persist(newScope);
            if (persisted) {
                return newScope;
            } else {
                log.error("Failed to persist scope, id:{}" + scopeId);
            }
        }

        throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, UmaErrorResponseType.INVALID_RESOURCE_SCOPE, "Failed to persist scope.");
    }

    private Filter createAnyFilterByIds(List<String> scopeIds) {
        if (scopeIds != null && !scopeIds.isEmpty()) {
        	List<Filter> filters = new ArrayList<Filter>();
            for (String url : scopeIds) {
            	Filter filter = Filter.createEqualityFilter("oxId", url);
            	filters.add(filter);
            }
            Filter filter = Filter.createORFilter(filters.toArray(new Filter[0]));
            log.trace("Uma scope ids: " + scopeIds + ", ldapFilter: " + filter);
            return filter;
        }

        return null;
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScopes();
    }

    public static String asString(Collection<Scope> scopes) {
        String result = "";
        for (Scope scope : scopes) {
            result += scope.getId() + " ";
        }
        return result.trim();
    }
}
