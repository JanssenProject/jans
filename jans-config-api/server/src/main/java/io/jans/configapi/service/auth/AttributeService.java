
package io.jans.configapi.service.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.GluuAttribute;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class AttributeService extends io.jans.as.common.service.AttributeService {

    private static final long serialVersionUID = -820393743995746612L;
    
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public PagedResult<GluuAttribute> searchGluuAttributes(SearchRequest searchRequest, String status) {
        log.info("Search GluuAttributes with searchRequest:{}, status:{}", escapeLog(searchRequest), status);

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
        
        log.trace("Attributes pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if(searchRequest.getFieldValueMap()!=null && !searchRequest.getFieldValueMap().isEmpty())
        {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }  
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters), Filter.createANDFilter(fieldValueFilters));
        }        

        log.trace("Attributes pattern and field searchFilter:{}", searchFilter);
       
        if (activeFilter != null) {
            searchFilter = Filter.createANDFilter(searchFilter, activeFilter);
        }

        log.info("GluuAttributes final searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForAttribute(null), GluuAttribute.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public GluuAttribute getAttributeUsingDn(String dn) {
        GluuAttribute result = null;
        try {
            result = persistenceEntryManager.find(GluuAttribute.class, dn);
        } catch (Exception ex) {
            log.error("Failed to load attribute with dn:{}, ex:{}", dn, ex);
        }
        return result;
    }

    public GluuAttribute getAttributeUsingName(String claimName) {
        GluuAttribute gluuAttribute = null;
        try {
            gluuAttribute = getByClaimName(claimName);
        } catch (Exception ex) {
            log.error("Failed to load attribute with name:{}, ex:{}", claimName, ex);
        }
        return gluuAttribute;
    }

}