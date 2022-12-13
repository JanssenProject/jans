
package io.jans.configapi.service.auth;

import io.jans.model.GluuAttribute;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class AttributeService extends io.jans.as.common.service.AttributeService {

    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public PagedResult<GluuAttribute> searchGluuAttributes(SearchRequest searchRequest, String status) {
        log.debug("Search GluuAttributes with searchRequest:{}, status:{}", searchRequest, status);

        Filter activeFilter = null;
        if (ApiConstants.ACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "active");
        } else if (ApiConstants.INACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "inactive");
        }

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter nameFilter = Filter.createSubstringFilter(AttributeConstants.JANS_ATTR_NAME, null, targetArray,
                        null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        if (activeFilter != null) {
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters), activeFilter);
        }

        log.debug("GluuAttributes to be fetched with searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForAttribute(null), GluuAttribute.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

}