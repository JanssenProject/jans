/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.jans.model.SearchRequest;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.service.OrganizationService;
import io.jans.service.custom.script.AbstractCustomScriptService;
import io.jans.util.OxConstants;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 09/07/2020
 */
@ApplicationScoped
public class CustomScriptService extends AbstractCustomScriptService {

    private static final long serialVersionUID = -7670016078535552193L;

    @Inject
    private OrganizationService organizationService;

    public String baseDn() {
        return String.format("ou=scripts,%s", organizationService.getDnForOrganization(null));
    }

    public PagedResult<CustomScript> searchScripts(String pattern, String sortBy, String sortOrder, Integer startIndex,
            int limit, int maximumRecCount, CustomScriptType type) {
        log.debug(
                "Search CustomScript with param - pattern:{}, sortBy:{}, sortOrder:{}, startIndex:{}, limit:{}, maximumRecCount:{}, type:{}",
                pattern, sortBy, sortOrder, startIndex, limit, maximumRecCount, type);

        Filter searchFilter = null;
        String[] targetArray = new String[] { pattern };

        boolean useLowercaseFilter = isLowercaseFilter(baseDn());
        if (useLowercaseFilter) {
            searchFilter = Filter.createORFilter(
                    Filter.createSubstringFilter(Filter.createLowercaseFilter(OxConstants.DESCRIPTION), null,
                            targetArray, null),
                    Filter.createSubstringFilter(Filter.createLowercaseFilter(OxConstants.DISPLAY_NAME), null,
                            targetArray, null));
        } else {
            searchFilter = Filter.createORFilter(
                    Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null),
                    Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null));
        }

        Filter filter = searchFilter;
        if (type != null) {
            Filter typeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
            filter = Filter.createANDFilter(searchFilter, typeFilter);
        }

        return persistenceEntryManager.findPagedEntries(baseDn(), CustomScript.class, filter, null, sortBy,
                SortOrder.getByValue(sortOrder), startIndex, limit, maximumRecCount);

    }

    public PagedResult<CustomScript> searchScripts(SearchRequest searchRequest, CustomScriptType type) {
        log.info("Search CustomScript with searchRequest - searchRequest:{}, type:{}", searchRequest, type);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();

        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                boolean useLowercaseFilter = isLowercaseFilter(baseDn());
                if (useLowercaseFilter) {
                    filters.add(Filter.createORFilter(
                            Filter.createSubstringFilter(Filter.createLowercaseFilter(OxConstants.DESCRIPTION), null,
                                    targetArray, null),
                            Filter.createSubstringFilter(Filter.createLowercaseFilter(OxConstants.DISPLAY_NAME), null,
                                    targetArray, null)));
                } else {
                    filters.add(Filter.createORFilter(
                            Filter.createSubstringFilter(OxConstants.DESCRIPTION, null, targetArray, null),
                            Filter.createSubstringFilter(OxConstants.DISPLAY_NAME, null, targetArray, null)));
                }

            }
            searchFilter = Filter.createORFilter(filters);
        }

        log.trace("CustomScript pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("CustomScript dataFilter:{}", dataFilter);
                log.trace("CustomScript entry.getKey():{}, entry.getValue():{}, entry.getValue().getClass():{}",
                        entry.getKey(), entry.getValue(),
                        (entry.getValue() != null ? entry.getValue().getClass() : " "));
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.trace("CustomScript pattern and field searchFilter:{}", searchFilter);

        Filter filter = searchFilter;
        log.debug("filter:{}", filter);
        if (type != null) {
            Filter typeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
            filter = Filter.createANDFilter(searchFilter, typeFilter);
        }

        log.trace("Searching CustomScript Flow with filter:{}", filter);
        return persistenceEntryManager.findPagedEntries(baseDn(), CustomScript.class, filter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public List<CustomScript> findActiveCustomScripts() {
        Filter filter = Filter.createEqualityFilter("jansEnabled", true);
        List<CustomScript> activeScripts = persistenceEntryManager.findEntries(baseDn(), CustomScript.class, filter, 0);
        log.debug(" activeScripts:{}", activeScripts);
        return activeScripts;
    }

}
