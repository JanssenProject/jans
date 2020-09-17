package org.gluu.configapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.OrganizationService;
import org.gluu.oxauth.util.OxConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

/**
 * Responsible for OpenID Connect, OAuth2 and UMA scopes. (Type is defined by
 * ScopeType.)
 *
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ScopeService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    StaticConfiguration staticConfiguration;

    @Inject
    OrganizationService organizationService;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScopes();
    }

    public String createDn(String inum) {
        return String.format("inum=%s,%s", inum, baseDn());
    }

    public void persist(Scope scope) {
        if (StringUtils.isBlank(scope.getDn())) {
            scope.setDn(createDn(scope.getInum()));
        }

        persistenceEntryManager.persist(scope);
    }

    public void addScope(Scope scope) throws Exception {
        persistenceEntryManager.persist(scope);
    }

    public void removeScope(Scope scope) throws Exception {
        persistenceEntryManager.remove(scope);
    }

    public void updateScope(Scope scope) throws Exception {
        persistenceEntryManager.merge(scope);
    }

    public Scope getScopeByInum(String inum) {
        Scope result = null;
        try {
            result = persistenceEntryManager.find(Scope.class, getDnForScope(inum));
        } catch (Exception e) {
            logger.debug("", e);
        }
        return result;
    }

    public String getDnForScope(String inum) throws Exception {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=scopes,%s", orgDn);
        }
        return String.format("inum=%s,ou=scopes,%s", inum, orgDn);
    }

    public String generateInumForNewScope() throws Exception {
        Scope scope = new Scope();
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForScope(newInum);
            scope.setDn(newDn);
        } while (persistenceEntryManager.contains(newDn, Scope.class));
        return newInum;
    }

    public List<Scope> searchScopes(String pattern, int sizeLimit) {
        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.description, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter, sizeLimit);
        } catch (Exception e) {
            logger.error("", e);
            return new ArrayList<Scope>();
        }
       
    }

    public List<Scope> getAllScopesList(int size) {
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, null, size);
        } catch (Exception e) {
            logger.error("", e);
            return new ArrayList<Scope>();
        }
    }

}
