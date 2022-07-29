/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.rest.model.CustomScope;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Inject
    ClientService clientService;

    @Inject
    UmaResourceService umaResourceService;

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

    public CustomScope getScopeByInum(String inum){
        return getScopeByInum(inum, false);
    }

    public CustomScope getScopeByInum(String inum, boolean withAssociatedClients) {
        try {
            CustomScope scope = persistenceEntryManager.find(CustomScope.class, getDnForScope(inum));
            if (withAssociatedClients) {
                List<Client> clients = clientService.getAllClients();
                List<UmaResource> umaResources = umaResourceService.getAllResources();
                return setClients(scope, clients, umaResources);
            }
            return scope;
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

    public List<CustomScope> searchScopes(String pattern, int sizeLimit) {
        return searchScopes(pattern, sizeLimit, null);
    }

    public List<Scope> searchScopesById(String jsId) {
        Filter searchFilter = Filter.createEqualityFilter(AttributeConstants.JANS_ID, jsId);
        try {
            return persistenceEntryManager.findEntries(getDnForScope(null), Scope.class, searchFilter);
        } catch (Exception e) {
            logger.error("No scopes found by pattern: " + jsId, e);
            return new ArrayList<>();
        }
    }

    public Scope getScopeByDn(String dn) {
        return persistenceEntryManager.find(Scope.class, dn);
    }

    public List<CustomScope> searchScopes(String pattern, int sizeLimit, String scopeType) {
        return searchScopes(pattern, sizeLimit, scopeType, false);
    }

    public List<CustomScope> searchScopes(String pattern, int sizeLimit, String scopeType, boolean withAssociatedClients) {
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createANDFilter(Filter.createEqualityFilter("jansScopeTyp", scopeType), searchFilter);
        }
        try {
            List<CustomScope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), CustomScope.class, searchFilter, sizeLimit);

            if (withAssociatedClients) {
                List<Client> clients = clientService.getAllClients();
                List<UmaResource> umaResources = umaResourceService.getAllResources();
                List<CustomScope> custScopes = scopes.stream().map(scope -> setClients(scope, clients, umaResources)).collect(Collectors.toList());
                return custScopes;
            }

            return scopes;
        } catch (Exception e) {
            logger.error("No scopes found by pattern: " + pattern, e);
            return new ArrayList<>();
        }
    }

    public List<CustomScope> getAllScopesList(int size) {
        return getAllScopesList(size, null);
    }

    public List<CustomScope> getAllScopesList(int size, String scopeType) {
        return getAllScopesList(size, scopeType, false);
    }

    public List<CustomScope> getAllScopesList(int size, String scopeType, boolean withAssociatedClients) {
        Filter searchFilter = null;
        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createEqualityFilter("jansScopeTyp", scopeType);
        }
        List<CustomScope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), CustomScope.class, searchFilter, size);

        if (withAssociatedClients) {
            List<Client> clients = clientService.getAllClients();
            List<UmaResource> umaResources = umaResourceService.getAllResources();
            List<CustomScope> custScopes = scopes.stream().map(scope -> setClients(scope, clients, umaResources)).collect(Collectors.toList());
            return custScopes;
        }
        return scopes;
    }

    private CustomScope setClients(Scope scope, List<Client> clients, List<UmaResource> umaResources) {
        ObjectMapper mapper = new ObjectMapper();
        CustomScope customScope = mapper.convertValue(scope, CustomScope.class);
        customScope.setClients(Lists.newArrayList());

        for (Client client : clients) {
            if (client.getScopes() == null) {
                continue;
            }
            if (scope.getScopeType() == ScopeType.OPENID || scope.getScopeType() == ScopeType.OAUTH || scope.getScopeType() == ScopeType.DYNAMIC) {
                if (Arrays.asList(client.getScopes()).contains(getDnForScope(scope.getInum()))) {
                    customScope.getClients().add(client);
                }
            } else if (scope.getScopeType() == ScopeType.UMA) {
                List<UmaResource> umaRes = umaResources.stream().filter(umaResource -> (umaResource.getScopes() != null && umaResource.getScopes().contains(getDnForScope(scope.getInum())))).collect(Collectors.toList());
                if (umaRes.stream().anyMatch(ele -> ele.getClients().contains(clientService.getDnForClient(client.getClientId())))) {
                    customScope.getClients().add(client);
                }
            } else if (scope.getScopeType() == ScopeType.SPONTANEOUS) {
                if (client.getClientId().equals(customScope.getAttributes().getSpontaneousClientId())) {
                    customScope.getClients().add(client);
                }
            }
        }
        return customScope;
    }
}
