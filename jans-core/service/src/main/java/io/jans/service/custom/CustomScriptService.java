/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;

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

    public PagedResult<CustomScript> searchScripts(SearchRequest searchRequest, CustomScriptType type) {
        log.debug("Search CustomScript with searchRequest:{}, type:{}", searchRequest, type);

        Filter searchFilter = null;
        if (StringUtils.isNotBlank(searchRequest.getFilter())) {
            String[] targetArray = new String[] { searchRequest.getFilter() };

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
        }
        Filter typeFilter = null;
        if (type != null) {
            typeFilter = Filter.createEqualityFilter(OxConstants.SCRIPT_TYPE, type);
        }
        Filter filter = Filter.createANDFilter(searchFilter, typeFilter);
        log.debug("Searching CustomScript Flow with filter:{}", filter);

        return persistenceEntryManager.findPagedEntries(baseDn(), CustomScript.class, filter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex() - 1, searchRequest.getCount(), searchRequest.getMaxCount());

    }

}
