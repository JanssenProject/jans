/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.model.SearchRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

@ApplicationScoped
public class SamlService {

    @Inject
    Logger log;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    private static final String SAML_DN_BASE = "ou=jansSAMLconfig,o=jans";

    public String baseDn() {
        // return staticConfiguration.getBaseDn().getTrustRelationshipDn();
        return SAML_DN_BASE;
    }

    public boolean contains(String dn) {
        return persistenceEntryManager.contains(dn, TrustRelationship.class);
    }
    
    public TrustRelationship getRelationshipByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(TrustRelationship.class, dn);
            } catch (Exception e) {
                log.info(e.getMessage());
            }

        }
        return null;
    }

    public TrustRelationship getTrustRelationshipByInum(String inum) {
        TrustRelationship result = null;
        try {
            result = persistenceEntryManager.find(TrustRelationship.class, getDnForTrustRelationship(inum));
        } catch (Exception ex) {
            log.error("Failed to load TrustRelationship entry", ex);
        }
        return result;
    }
    
    public TrustRelationship getTrustContainerFederation(TrustRelationship trustRelationship) {
        TrustRelationship relationshipByDn = getRelationshipByDn(trustRelationship.getDn());
        return relationshipByDn;
    }

    public TrustRelationship getTrustContainerFederation(String dn) {
        TrustRelationship relationshipByDn = getRelationshipByDn(dn);
        return relationshipByDn;
    }

    public List<TrustRelationship> getAllSAMLTrustRelationships(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                null, sizeLimit);
    }

    public List<TrustRelationship> searchTrustRelationship(String pattern, int sizeLimit) {

        log.debug("Search TrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        log.debug("Search TrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                searchFilter, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                null, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship() {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                null);
    }

    public PagedResult<Client> getTrustRelationship(SearchRequest searchRequest) {
        log.debug("Search TrustRelationship with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        log.trace("TrustRelationship pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("TrustRelationship dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.debug("TrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTrustRelationship(null), Client.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship) {
        setTrustRelationship(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public void removeTrustRelationship(TrustRelationship trustRelationship) {
        persistenceEntryManager.removeRecursively(trustRelationship.getDn(), TrustRelationship.class);

    }

    public TrustRelationship updateTrustRelationship(TrustRelationship trustRelationship) {
        setTrustRelationship(trustRelationship, true);
        persistenceEntryManager.merge(trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    public TrustRelationship setTrustRelationship(TrustRelationship trustRelationship, boolean update) {
        return trustRelationship;
    }

    public String getDnForTrustRelationship(String inum) {
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=jansSAMLconfig,%s", SAML_DN_BASE);
        }
        return String.format("inum=%s,ou=jansSAMLconfig,%s", inum, SAML_DN_BASE);
    }

}
