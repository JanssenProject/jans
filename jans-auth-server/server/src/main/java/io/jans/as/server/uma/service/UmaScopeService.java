/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.common.CreatorType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.service.SpontaneousScopeService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

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

    @Inject
    private SpontaneousScopeService spontaneousScopeService;

    public static String asString(Collection<Scope> scopes) {
        StringBuilder result = new StringBuilder();
        for (Scope scope : scopes) {
            result.append(scope.getId()).append(" ");
        }
        return result.toString().trim();
    }

    public Scope getOrCreate(Client client, String scopeId, Set<String> regExps) {
        Scope fromLdap = getScope(scopeId);
        if (fromLdap != null) { // already exists
            return fromLdap;
        }

        if (isFalse(appConfiguration.getAllowSpontaneousScopes())) {
            return null;
        }

        if (isFalse(client.getAttributes().getAllowSpontaneousScopes())) {
            return null;
        }

        if (!spontaneousScopeService.isAllowedBySpontaneousScopeRegExps(regExps, scopeId)) {
            return null;
        }

        return spontaneousScopeService.createSpontaneousScopeIfNeeded(regExps, scopeId, client.getClientId());
    }

    public Scope getScope(String scopeId) {
        try {
            final Filter filter = Filter.createEqualityFilter("jansId", scopeId);
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

    public List<String> getScopeDNsByIdsAndAddToPersistenceIfNeeded(List<String> scopeIds) {
        List<String> result = new ArrayList<>();
        for (Scope scope : getScopesByIds(scopeIds)) {
            result.add(scope.getDn());
        }
        return result;
    }

    public List<Scope> getScopesByDns(List<String> scopeDns) {
        final List<Scope> result = new ArrayList<>();
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
        final List<String> result = new ArrayList<>();
        if (scopes != null && !scopes.isEmpty()) {
            for (Scope scope : scopes) {
                result.add(scope.getId());
            }
        }
        return result;
    }

    public List<Scope> getScopesByIds(List<String> scopeIds) {
        List<Scope> result = new ArrayList<>();
        if (scopeIds != null && !scopeIds.isEmpty()) {
            List<String> notInLdap = new ArrayList<>(scopeIds);

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
            newScope.setDeletable(false);
            newScope.setCreatorType(CreatorType.AUTO);

            final boolean persisted = persist(newScope);
            if (persisted) {
                return newScope;
            } else {
                log.error("Failed to persist scope, id: {}", scopeId);
            }
        }

        throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, UmaErrorResponseType.INVALID_SCOPE, "Failed to persist scope.");
    }

    private Filter createAnyFilterByIds(List<String> scopeIds) {
        if (scopeIds != null && !scopeIds.isEmpty()) {
            List<Filter> filters = new ArrayList<>();
            for (String url : scopeIds) {
                Filter filter = Filter.createEqualityFilter("jansId", url);
                filters.add(filter);
            }
            Filter filter = Filter.createORFilter(filters.toArray(new Filter[0]));
            log.trace("Uma scope ids: {}, ldapFilter: {}", scopeIds, filter);
            return filter;
        }

        return null;
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScopes();
    }
}
