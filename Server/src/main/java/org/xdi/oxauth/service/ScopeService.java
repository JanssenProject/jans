/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.ArrayList;
import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;

import com.unboundid.ldap.sdk.Filter;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 */
@Scope(ScopeType.STATELESS)
@Name("scopeService")
@AutoCreate
public class ScopeService {

    @In
    LdapEntryManager ldapEntryManager;

    @Logger
    private Log log;

    /**
     * Get ScopeService instance
     *
     * @return ScopeService instance
     */
    public static ScopeService instance() {
        boolean createContexts = !Contexts.isEventContextActive() && !Contexts.isApplicationContextActive();
        if (createContexts) {
            Lifecycle.beginCall();
        }

        return (ScopeService) Component.getInstance(ScopeService.class);
    }

    /**
     * returns a list of all scopes
     *
     * @return list of scopes
     */
    public List<org.xdi.oxauth.model.common.Scope> getAllScopesList() {
        String scopesBaseDN = ConfigurationFactory.instance().getBaseDn().getScopes();

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
        return ldapEntryManager.find(org.xdi.oxauth.model.common.Scope.class, dn);
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
    public org.xdi.oxauth.model.common.Scope getScopeByDisplayName(String DisplayName) {
        String scopesBaseDN = ConfigurationFactory.instance().getBaseDn().getScopes();

        org.xdi.oxauth.model.common.Scope scope = new org.xdi.oxauth.model.common.Scope();
        scope.setDn(scopesBaseDN);
        scope.setDisplayName(DisplayName);

        List<org.xdi.oxauth.model.common.Scope> scopes = ldapEntryManager.findEntries(scope);
        if ((scopes != null) && (scopes.size() > 0)) {
            return scopes.get(0);
        }

        return null;
    }
}