/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.google.api.client.util.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.rest.model.CustomScope;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static final String JANS_SCOPE_TYP = "jansScopeTyp";
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

    public Scope getScope(String inum) {
        try {
            return persistenceEntryManager.find(Scope.class, getDnForScope(inum));
        } catch (Exception ex) {
            logger.error("Error while finding scope with inum:{} is:{}", inum, ex);
        }
        return null;
    }

    public CustomScope getScopeByInum(String inum) {
        return getScopeByInum(inum, false);
    }

    public CustomScope getScopeByInum(String inum, boolean withAssociatedClients) {
        try {
            CustomScope scope = persistenceEntryManager.find(CustomScope.class, getDnForScope(inum));
            if (withAssociatedClients) {

                return setClients(scope);
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

    public List<CustomScope> searchScopes(String pattern, int sizeLimit, String scopeType,
            boolean withAssociatedClients) {
        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createANDFilter(Filter.createEqualityFilter(JANS_SCOPE_TYP, scopeType), searchFilter);
        }
        try {
            List<CustomScope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), CustomScope.class,
                    searchFilter, sizeLimit);

            if (withAssociatedClients) {
                getAssociatedClients(scopes);
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
            searchFilter = Filter.createEqualityFilter(JANS_SCOPE_TYP, scopeType);
        }
        List<CustomScope> scopes = persistenceEntryManager.findEntries(getDnForScope(null), CustomScope.class,
                searchFilter, size);

        if (withAssociatedClients) {
            getAssociatedClients(scopes);
        }
        return scopes;
    }

    public List<CustomScope> searchScope(SearchRequest searchRequest) {
        logger.debug("Search Scope with searchRequest:{}", searchRequest);

        if (searchRequest != null && searchRequest.getFilterAttributeName() != null
                && !searchRequest.getFilterAttributeName().isEmpty()) {

            ArrayList<Filter> searchFilters = new ArrayList<>();

            for (String filterAttribute : searchRequest.getFilterAttributeName()) {
                Filter filter = Filter.createEqualityFilter(filterAttribute, searchRequest.getFilter());
                searchFilters.add(filter);
            }
            logger.debug("Search Scope with searchFilters:{}", searchFilters);
            return persistenceEntryManager.findEntries(getDnForScope(null), CustomScope.class,
                    Filter.createANDFilter(searchFilters));

        }

        return Collections.emptyList();
    }

    public List<Scope> getAllScopesList() {
        String scopesBaseDN = staticConfiguration.getBaseDn().getScopes();

        return persistenceEntryManager.findEntries(scopesBaseDN, Scope.class, Filter.createPresenceFilter("inum"));
    }

    public List<String> getDefaultScopesDn() {
        List<String> defaultScopes = new ArrayList<>();

        for (Scope scope : getAllScopesList()) {
            if (Boolean.TRUE.equals(scope.isDefaultScope())) {
                defaultScopes.add(scope.getDn());
            }
        }

        return defaultScopes;
    }

    public List<String> getScopesDn(List<String> scopeDnList) {
        List<String> scopes = new ArrayList<>();

        for (String scopeDn : scopeDnList) {
            Scope scope = getScopeByDn(scopeDn);
            if (scope != null) {
                scopes.add(scope.getDn());
            }
        }

        return scopes;
    }

    private CustomScope setClients(CustomScope customScope) {
        logger.debug("Getting associated-clients for scope - customScope:{}", customScope);

        List<Client> clients = clientService.getAllClients();
        List<UmaResource> umaResources = umaResourceService.getAllResources();
        logger.debug("Verifying associated-clients using clients:{}, umaResources:{}", clients, umaResources);
        customScope.setClients(Lists.newArrayList());

        for (Client client : clients) {
            logger.debug(
                    "Associated clients search - customScope.getScopeType():{}, customScope.getInum():{}, customScope.getCreatorId():{}, client.getClientId():{}, clientService.getDnForClient(client.getClientId()):{}, client.getScopes():{}, client.getClientId().equals(customScope.getCreatorId()):{}",
                    customScope.getScopeType(), customScope.getInum(), customScope.getCreatorId(), client.getClientId(),
                    clientService.getDnForClient(client.getClientId()), client.getScopes(),
                    client.getClientId().equals(customScope.getCreatorId()));

            if (customScope.getScopeType() == ScopeType.OPENID || customScope.getScopeType() == ScopeType.OAUTH
                    || customScope.getScopeType() == ScopeType.DYNAMIC) {
                if (client.getScopes() != null
                        && Arrays.asList(client.getScopes()).contains(getDnForScope(customScope.getInum()))) {
                    logger.debug(
                            "Associated clients match for OOD - customScope.getScopeType():{}, customScope.getInum():{},client.getClientId():{}",
                            customScope.getScopeType(), customScope.getInum(), client.getClientId());
                    customScope.getClients().add(client);
                }
            } else if (customScope.getScopeType() == ScopeType.UMA) {
                List<UmaResource> umaRes = umaResources.stream()
                        .filter(umaResource -> (umaResource.getScopes() != null
                                && umaResource.getScopes().contains(getDnForScope(customScope.getInum()))))
                        .collect(Collectors.toList());
                logger.trace("Associated clients search - umaRes():{}", umaRes);
                if (umaRes.stream().anyMatch(
                        ele -> ele.getClients().contains(clientService.getDnForClient(client.getClientId())))) {
                    customScope.getClients().add(client);

                }
            } else if ((customScope.getScopeType() == ScopeType.SPONTANEOUS)
                    && (client.getClientId().equals(customScope.getCreatorId()))) {
                logger.debug(
                        "Associated clients match for SPONTANEOUS - customScope.getScopeType():{}, customScope.getInum():{},customScope.getCreatorId():{}, client.getClientId():{}",
                        customScope.getScopeType(), customScope.getInum(), customScope.getCreatorId(),
                        client.getClientId());
                customScope.getClients().add(client);
            }
        }
        return customScope;
    }

    public List<CustomScope> getAssociatedClients(List<CustomScope> scopes) {
        logger.debug("Getting associatedClients for scopes:{}", scopes);
        if (scopes == null) {
            return scopes;
        }

        List<CustomScope> scopeList = Lists.newArrayList();
        for (CustomScope scope : scopes) {
            scopeList.add(setClients(scope));
        }

        logger.debug("Getting associatedClients for scopeList:{}", scopeList);
        return scopeList;

    }

    public PagedResult<CustomScope> getScopeResult(SearchRequest searchRequest, String scopeType,
            boolean withAssociatedClients) {
        logger.debug("Search Scope with searchRequest:{}, scopeType:{}, withAssociatedClients:{}", searchRequest,
                scopeType, withAssociatedClients);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter nameFilter = Filter.createSubstringFilter(AttributeConstants.JANS_ID, null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        if (StringHelper.isNotEmpty(scopeType)) {
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createEqualityFilter(JANS_SCOPE_TYP, scopeType));
        }

        logger.debug("Final Scope searchFilter:{}", searchFilter);

        PagedResult<CustomScope> pagedResult = persistenceEntryManager.findPagedEntries(getDnForScope(null),
                CustomScope.class, searchFilter, null, searchRequest.getSortBy(),
                SortOrder.getByValue(searchRequest.getSortOrder()), searchRequest.getStartIndex(),
                searchRequest.getCount(), searchRequest.getMaxCount());

        if (pagedResult != null) {
            logger.debug(
                    "Scope fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());

            List<CustomScope> scopes = pagedResult.getEntries();
            if (withAssociatedClients) {
                getAssociatedClients(scopes);
            }

            logger.debug("scopes:{}", scopes);
            pagedResult.setEntries(scopes);
        }
        return pagedResult;
    }
}
