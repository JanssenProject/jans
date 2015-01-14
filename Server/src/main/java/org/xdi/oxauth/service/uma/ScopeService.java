/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.InternalExternal;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.model.uma.persistence.UmaScopeType;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.uma.ws.rs.MetaDataConfigurationRestWebServiceImpl;
import org.xdi.oxauth.util.ServerUtil;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 22/04/2013
 */
@AutoCreate
@Scope(ScopeType.STATELESS)
@Name("umaScopeService")
public class ScopeService {

    @Logger
    private Log log;
    @In
    private LdapEntryManager ldapEntryManager;
    @In
    private InumService inumService;
    @In
    private ErrorResponseFactory errorResponseFactory;

    public static ScopeService instance() {
        return ServerUtil.instance(ScopeService.class);
    }

    public List<ScopeDescription> getAllScopes() {
        try {
            return ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, Filter.createPresenceFilter("inum"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<ScopeDescription> getScopes(UmaScopeType p_type) {
        try {
            if (p_type != null) {
                final Filter filter = Filter.create(String.format("&(oxType=%s)", p_type.getValue()));
                return ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, filter);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public ScopeDescription getInternalScope(String p_scopeId) {
        try {
            final Filter filter = Filter.create(String.format("&(oxType=%s)(oxId=%s)", UmaScopeType.INTERNAL.getValue(), p_scopeId));
            final List<ScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, filter);
            if (entries != null && !entries.isEmpty()) {

                // if more then one scope then it's problem, non-deterministic behavior, id must be unique
                if (entries.size() > 1) {
                    log.error("Found more then one internal uma scope by input id: {0}" + p_scopeId);
                    for (ScopeDescription s : entries) {
                        log.error("Scope, Id: {0}, dn: {1}", s.getId(), s.getDn());
                    }
                }
                return entries.get(0);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean persist(ScopeDescription p_scopeDescription) {
        try {
            if (StringUtils.isBlank(p_scopeDescription.getDn())) {
                p_scopeDescription.setDn(String.format("inum=%s,%s", p_scopeDescription.getInum(), baseDn()));
            }

            ldapEntryManager.persist(p_scopeDescription);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<String> getScopeDNsByUrlsAndAddToLdapIfNeeded(List<String> p_scopeUrls) {
        final List<String> result = new ArrayList<String>();
        if (p_scopeUrls != null && !p_scopeUrls.isEmpty()) {
            try {
                List<String> sourceScopeUrls = handleInternalScopes(p_scopeUrls, result);
                if (sourceScopeUrls.size() > 0) {
                	handleExternalScopes(sourceScopeUrls, result);
                }
            } catch (WebApplicationException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    private List<String> handleInternalScopes(List<String> p_scopeUrls, List<String> result) {
    	List<String> notProcessedScopeUrls = new ArrayList<String>(p_scopeUrls);
        try {
            final Filter filter = Filter.create(String.format("&(oxType=%s)", InternalExternal.INTERNAL.getValue()));
            final List<ScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, filter);
            if (entries != null && !entries.isEmpty()) {
                for (String scopeUrl : p_scopeUrls) {
                    for (ScopeDescription scopeDescription : entries) {
                        final String internalScopeUrl = getInternalScopeUrl(scopeDescription);
                        if (internalScopeUrl.equals(scopeUrl) && !result.contains(internalScopeUrl)) {
                            result.add(scopeDescription.getDn());
                            notProcessedScopeUrls.remove(scopeUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
        return notProcessedScopeUrls;
    }

    private void handleExternalScopes(List<String> p_scopeUrls, List<String> result) throws LDAPException {
        for (String scopeUrl : p_scopeUrls) {
            final Filter filter = Filter.create(String.format("&(oxUrl=%s)", scopeUrl));
            final List<ScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, filter);
            if (entries != null && !entries.isEmpty()) {
                result.add(entries.get(0).getDn());
            } else { // scope is not in ldap, add it dynamically

                final Boolean addAutomatically = ConfigurationFactory.getConfiguration().getUmaAddScopesAutomatically();

                if (addAutomatically != null && addAutomatically) {
                    final String inum = inumService.generateInum();
                    final ScopeDescription newScope = new ScopeDescription();
                    newScope.setInum(inum);
                    newScope.setUrl(scopeUrl);
                    newScope.setDisplayName(scopeUrl); // temp solution : need extract info from scope description on resource server
                    newScope.setId(UmaScopeType.EXTERNAL_AUTO.getValue());  // dummy id : not sure what to put right now as id is required by @NotNull annotation
                    newScope.setType(InternalExternal.EXTERNAL_AUTO);

                    final boolean persisted = persist(newScope);
                    if (persisted) {
                        result.add(newScope.getDn());
                    }
                } else {
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.INVALID_RESOURCE_SET_SCOPE)).build());
                }
            }
        }
    }

    public List<ScopeDescription> getScopesByUrls(List<String> p_scopeUrls) {
        try {
            final Filter filter = createAnyFilterByUrls(p_scopeUrls);
            if (filter != null) {
                final List<ScopeDescription> entries = ldapEntryManager.findEntries(baseDn(), ScopeDescription.class, filter);
                if (entries != null) {
                    return entries;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    // TODO: Optimize scopes loading. It's possible to loads all scope in one request.
    public List<ScopeDescription> getScopesByDns(List<String> p_scopeDns) {
        final List<ScopeDescription> result = new ArrayList<ScopeDescription>();
        try {
            if (p_scopeDns != null && !p_scopeDns.isEmpty()) {
                for (String dn : p_scopeDns) {
                    final ScopeDescription scopeDescription = ldapEntryManager.find(ScopeDescription.class, dn);
                    if (scopeDescription != null) {
                        result.add(scopeDescription);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public List<String> getScopeUrlsByDns(List<String> p_scopeDns) {
        return getScopeUrls(getScopesByDns(p_scopeDns));
    }

    public static List<String> getScopeDNs(List<ScopeDescription> p_scopes) {
        final List<String> result = new ArrayList<String>();
        if (p_scopes != null && !p_scopes.isEmpty()) {
            for (ScopeDescription s : p_scopes) {
                result.add(s.getDn());
            }
        }
        return result;
    }

    public static List<String> getScopeUrls(List<ScopeDescription> p_scopes) {
        final List<String> result = new ArrayList<String>();
        if (p_scopes != null && !p_scopes.isEmpty()) {
            for (ScopeDescription s : p_scopes) {
                final InternalExternal type = s.getType();
                if (type != null) {
                    switch (type) {
                        case EXTERNAL:
                        case EXTERNAL_AUTO:
                            result.add(s.getUrl());
                            break;
                        case INTERNAL:
                            result.add(getInternalScopeUrl(s));
                            break;
                    }
                } else {
                    result.add(s.getUrl());
                }
            }
        }
        return result;
    }

    private static String getInternalScopeUrl(ScopeDescription internalScope) {
        if (internalScope != null && internalScope.getType() == InternalExternal.INTERNAL) {
            return getScopeEndpoint() + "/" + internalScope.getId();
        }
        return "";
    }

    private static String getScopeEndpoint() {
        return ConfigurationFactory.getConfiguration().getBaseEndpoint() + MetaDataConfigurationRestWebServiceImpl.UMA_SCOPES_SUFFIX;
    }

    private Filter createAnyFilterByUrls(List<String> p_scopeUrls) {
        try {
            if (p_scopeUrls != null && !p_scopeUrls.isEmpty()) {
                final StringBuilder sb = new StringBuilder("(|");
                for (String url : p_scopeUrls) {
                    sb.append("(");
                    sb.append("oxUrl=");
                    sb.append(url);
                    sb.append(")");
                }
                sb.append(")");
                final String filterAsString = sb.toString();
                log.trace("Uma scope urls: " + p_scopeUrls + ", ldapFilter: " + filterAsString);
                return Filter.create(filterAsString);
            }
        } catch (LDAPException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static String baseDn() {
        return String.format("ou=scopes,%s", ConfigurationFactory.getBaseDn().getUmaBase());
    }
}
