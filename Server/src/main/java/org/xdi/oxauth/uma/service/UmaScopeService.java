/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.uma.ws.rs.UmaMetadataWS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
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
    private LdapEntryManager ldapEntryManager;

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
            final Filter filter = Filter.create(String.format("&(oxId=%s)", scopeId));
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
        final List<String> result = new ArrayList<String>();
        if (scopeIds != null && !scopeIds.isEmpty()) {
            try {
                final Boolean addAutomatically = appConfiguration.getUmaAddScopesAutomatically();

                for (String scopeId : scopeIds) {
                    UmaScopeDescription scope = getScope(scopeId);
                    if (scope != null) {
                        result.add(scope.getDn());
                    } else {
                        if (addAutomatically != null && addAutomatically) {
                            final String inum = inumService.generateInum();
                            final UmaScopeDescription newScope = new UmaScopeDescription();
                            newScope.setInum(inum);
                            newScope.setDisplayName(scopeId);
                            newScope.setId(scopeId);

                            final boolean persisted = persist(newScope);
                            if (persisted) {
                                result.add(newScope.getDn());
                            } else {
                                log.error("Failed to persist scope, id:{}" + scopeId);
                            }
                        } else {
                            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.INVALID_RESOURCE_SCOPE)).build());
                        }
                    }
                }
            } catch (WebApplicationException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
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

    public static List<String> getScopeDNs(List<UmaScopeDescription> scopes) {
        final List<String> result = new ArrayList<String>();
        if (scopes != null && !scopes.isEmpty()) {
            for (UmaScopeDescription s : scopes) {
                result.add(s.getDn());
            }
        }
        return result;
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
        try {
            if (scopeIds != null && !scopeIds.isEmpty()) {
                final List<UmaScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), UmaScopeDescription.class, createAnyFilterByIds(scopeIds));
                if (entries != null) {
                    return entries;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public String getScopeEndpoint() {
        return appConfiguration.getBaseEndpoint() + UmaMetadataWS.UMA_SCOPES_SUFFIX;
    }

    private Filter createAnyFilterByIds(List<String> scopeIds) {
        try {
            if (scopeIds != null && !scopeIds.isEmpty()) {
                final StringBuilder sb = new StringBuilder("(|");
                for (String url : scopeIds) {
                    sb.append("(");
                    sb.append("oxId=");
                    sb.append(url);
                    sb.append(")");
                }
                sb.append(")");
                final String filterAsString = sb.toString();
                log.trace("Uma scope ids: " + scopeIds + ", ldapFilter: " + filterAsString);
                return Filter.create(filterAsString);
            }
        } catch (LDAPException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public String baseDn() {
        return String.format("ou=scopes,%s", staticConfiguration.getBaseDn().getUmaBase());
    }
}
