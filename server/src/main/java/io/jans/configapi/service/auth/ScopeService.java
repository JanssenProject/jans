/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.Scope;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
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
        return searchScopes(pattern, sizeLimit, null);
    }

    public List<Scope> searchScopesById(String jsId) {
        Filter searchFilter = Filter.createEqualityFilter(AttributeConstants.jsId,jsId);
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter);
        } catch (Exception e) {
            logger.error("No scopes found by pattern: " + jsId, e);
            return new ArrayList<>();
        }
    }

    public List<Scope> searchScopes(String pattern, int sizeLimit, String scopeType) {
        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.displayName, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.description, null, targetArray,
                null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createANDFilter(Filter.createEqualityFilter("jansScopeTyp", scopeType), searchFilter);
        }
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter, sizeLimit);
        } catch (Exception e) {
            logger.error("No scopes found by pattern: " + pattern, e);
            return new ArrayList<>();
        }
    }

    public List<Scope> getAllScopesList(int size) {
        return getAllScopesList(size, null);
    }

    public List<Scope> getAllScopesList(int size, String scopeType) {
        Filter searchFilter = null;
        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createEqualityFilter("jansScopeTyp", scopeType);
        }
        return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter, size);
    }
}
