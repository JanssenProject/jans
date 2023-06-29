/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.model.JansTrustRelationship;
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
    Logger logger;

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
        return persistenceEntryManager.contains(dn, JansTrustRelationship.class);
    }

    public JansTrustRelationship getJansTrustRelationshipByInum(String inum) {
        JansTrustRelationship result = null;
        try {
            result = persistenceEntryManager.find(JansTrustRelationship.class, getDnForJansTrustRelationship(inum));
        } catch (Exception ex) {
            logger.error("Failed to load JansTrustRelationship entry", ex);
        }
        return result;
    }

    public List<JansTrustRelationship> searchJansTrustRelationship(String pattern, int sizeLimit) {

        logger.debug("Search JansTrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search JansTrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForJansTrustRelationship(null), JansTrustRelationship.class,
                searchFilter, sizeLimit);
    }

    public List<JansTrustRelationship> getAllJansTrustRelationship(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForJansTrustRelationship(null), JansTrustRelationship.class,
                null, sizeLimit);
    }

    public List<JansTrustRelationship> getAllJansTrustRelationship() {
        return persistenceEntryManager.findEntries(getDnForJansTrustRelationship(null), JansTrustRelationship.class,
                null);
    }

    public PagedResult<Client> getJansTrustRelationship(SearchRequest searchRequest) {
        logger.debug("Search JansTrustRelationship with searchRequest:{}", searchRequest);

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

        logger.trace("JansTrustRelationship pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("JansTrustRelationship dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.debug("JansTrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForJansTrustRelationship(null), Client.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public JansTrustRelationship addTrustRelationship(JansTrustRelationship trustRelationship) {
        setJansTrustRelationship(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);
        return getJansTrustRelationshipByInum(trustRelationship.getInum());
    }

    public void removeTrustRelationship(JansTrustRelationship trustRelationship) {
        persistenceEntryManager.removeRecursively(trustRelationship.getDn(), JansTrustRelationship.class);

    }

    public JansTrustRelationship updateTrustRelationship(JansTrustRelationship trustRelationship) {
        setJansTrustRelationship(trustRelationship, true);
        persistenceEntryManager.merge(trustRelationship);
        return getJansTrustRelationshipByInum(trustRelationship.getInum());
    }

    public JansTrustRelationship setJansTrustRelationship(JansTrustRelationship trustRelationship, boolean update) {
        return trustRelationship;
    }

    public String getDnForJansTrustRelationship(String inum) {
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=jansSAMLconfig,%s", SAML_DN_BASE);
        }
        return String.format("inum=%s,ou=jansSAMLconfig,%s", inum, SAML_DN_BASE);
    }

}
