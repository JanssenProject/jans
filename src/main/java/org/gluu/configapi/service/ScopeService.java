package org.gluu.configapi.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.OrganizationService;
import org.gluu.oxauth.util.OxConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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

    public void addScope(Scope scope) {
        persistenceEntryManager.persist(scope);
    }

    public void removeScope(Scope scope) {
        persistenceEntryManager.remove(scope);
    }

    public void updateScope(Scope scope) {
        persistenceEntryManager.merge(scope);
    }

    public Scope getScopeByInum(String inum) {
        try {
            return persistenceEntryManager.find(Scope.class, getDnForScope(inum));
        } catch (Exception e) {
            return null;
        }
    }

    public String getDnForScope(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=scopes,%s", orgDn);
        }
        return String.format("inum=%s,ou=scopes,%s", inum, orgDn);
    }

    public List<Scope> searchScopes(String pattern, int sizeLimit) {
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.description, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter, sizeLimit);
        } catch (Exception e) {
            logger.error("No scopes found by pattern: " + pattern, e);
            return new ArrayList<>();
        }
    }

    public List<Scope> getAllScopesList(int size) {
        return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, null, size);
    }
}
