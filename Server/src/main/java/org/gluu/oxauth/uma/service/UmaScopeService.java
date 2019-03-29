/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.persistence.UmaScopeDescription;
import org.gluu.oxauth.uma.authorization.UmaWebException;
import org.gluu.oxauth.uma.ws.rs.UmaMetadataWS;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.InumService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    public List<UmaScopeDescription> getAllScopes() {
        try {
            return ldapEntryManager.findEntries(baseDn(), UmaScopeDescription.class, Filter.createPresenceFilter("inum"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public UmaScopeDescription getScope(String scopeId) {
        try {
            final Filter filter = Filter.createEqualityFilter("oxId", scopeId);
            final List<UmaScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), UmaScopeDescription.class, filter);
            if (entries != null && !entries.isEmpty()) {
                // if more then one scope then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one UMA scope, id: {}", scopeId);
                    for (UmaScopeDescription s : entries) {
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

    public boolean persist(UmaScopeDescription scope) {
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
        for (UmaScopeDescription scope : getScopesByIds(scopeIds)) {
            result.add(scope.getDn());
        }
        return result;
    }

    public List<UmaScopeDescription> getScopesByDns(List<String> scopeDns) {
        final List<UmaScopeDescription> result = new ArrayList<UmaScopeDescription>();
        try {
            if (scopeDns != null && !scopeDns.isEmpty()) {
                for (String dn : scopeDns) {
                    final UmaScopeDescription scopeDescription = ldapEntryManager.find(UmaScopeDescription.class, dn);
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

    public List<String> getScopeIds(List<UmaScopeDescription> scopes) {
        final List<String> result = new ArrayList<String>();
        if (scopes != null && !scopes.isEmpty()) {
            for (UmaScopeDescription scope : scopes) {
                result.add(scope.getId());
            }
        }
        return result;
    }

    public List<UmaScopeDescription> getScopesByIds(List<String> scopeIds) {
        List<UmaScopeDescription> result = new ArrayList<UmaScopeDescription>();
        if (scopeIds != null && !scopeIds.isEmpty()) {
            List<String> notInLdap = new ArrayList<String>(scopeIds);

            final List<UmaScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), UmaScopeDescription.class, createAnyFilterByIds(scopeIds));
            if (entries != null) {
                result.addAll(entries);
                for (UmaScopeDescription scope : entries) {
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

    public UmaScopeDescription addScope(String scopeId) {
        final Boolean addAutomatically = appConfiguration.getUmaAddScopesAutomatically();
        if (addAutomatically != null && addAutomatically) {
            final String inum = inumService.generateInum();
            final UmaScopeDescription newScope = new UmaScopeDescription();
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

        throw new UmaWebException(Response.Status.BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_RESOURCE_SCOPE);

    }

    public String getScopeEndpoint() {
        return appConfiguration.getBaseEndpoint() + UmaMetadataWS.UMA_SCOPES_SUFFIX;
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
        return String.format("ou=scopes,%s", staticConfiguration.getBaseDn().getUmaBase());
    }

    public static String asString(Collection<UmaScopeDescription> scopes) {
        String result = "";
        for (UmaScopeDescription scope : scopes) {
            result += scope.getId() + " ";
        }
        return result.trim();
    }
}
