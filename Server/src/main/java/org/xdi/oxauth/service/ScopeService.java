package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;

import java.util.List;

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
        String scopesBaseDN = ConfigurationFactory.getBaseDn().getScopes();

        return ldapEntryManager.findEntries(scopesBaseDN,
                org.xdi.oxauth.model.common.Scope.class,
                Filter.createPresenceFilter("inum"));
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
        String scopesBaseDN = ConfigurationFactory.getBaseDn().getScopes();

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