package io.jans.configapi.plugin.lock.service;


import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.lock.model.stat.TelemetryEntry;

import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;


@ApplicationScoped
public class AuditService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject
    OrganizationService organizationService;
    
    @Inject
    private transient InumService inumService;
          
    public TelemetryEntry addTelemetryData(TelemetryEntry telemetryEntry) {
        if(telemetryEntry == null) {
            return telemetryEntry;
        }
        String id = telemetryEntry.getId();
        if(StringUtils.isBlank(id)) {
            id = this.generateInumForNewTelemetryEntry();
            telemetryEntry.setId(id);
        }
        persistenceEntryManager.persist(telemetryEntry);
        
        telemetryEntry = this.getTelemetryEntryByDn(this.getDnForTelemetryEntry(id));
        return telemetryEntry;
    }

    public void removeTelemetryEntry(TelemetryEntry telemetryEntry) {
        persistenceEntryManager.removeRecursively(telemetryEntry.getDn(), TelemetryEntry.class);
    }

    public void updateTelemetryEntry(TelemetryEntry telemetryEntry) {
        persistenceEntryManager.merge(telemetryEntry);
    }

    public TelemetryEntry getTelemetryEntryByInum(String inum) {
        TelemetryEntry result = null;
        try {
            result = persistenceEntryManager.find(TelemetryEntry.class, getTelemetryEntryByDn(inum));
        } catch (Exception ex) {
            logger.error("Failed to load TelemetryEntry entry", ex);
        }
        return result;
    }

    public List<TelemetryEntry> searchTelemetryEntrys(String pattern, int sizeLimit) {

        logger.debug("Search TelemetryEntrys with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search TelemetryEntrys with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTelemetryEntry(null), TelemetryEntry.class, searchFilter, sizeLimit);
    }

    public List<TelemetryEntry> getAllTelemetryEntrys(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTelemetryEntry(null), TelemetryEntry.class, null, sizeLimit);
    }

    public List<TelemetryEntry> getAllTelemetryEntrys() {
        return persistenceEntryManager.findEntries(getDnForTelemetryEntry(null), TelemetryEntry.class, null);
    }

    public PagedResult<TelemetryEntry> getTelemetryEntrys(SearchRequest searchRequest) {
        logger.debug("Search TelemetryEntrys with searchRequest:{}", searchRequest);

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

        logger.trace("TelemetryEntrys pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("TelemetryEntrys dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.debug("TelemetryEntrys searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTelemetryEntry(null), TelemetryEntry.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public TelemetryEntry getTelemetryEntryByDn(String dn) {
        try {
            return persistenceEntryManager.find(TelemetryEntry.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    
    public String getDnForTelemetryEntry(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=lock-telemetry,%s", orgDn);
        }
        return String.format("inum=%s,ou=lock-telemetry,%s", inum, orgDn);
    }

    
    public String generateInumForNewTelemetryEntry() {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < InumService.MAX_IDGEN_TRY_COUNT) {
                newInum = inumService.generateId("telemetry");
                trycount++;
            } else {
                newInum = inumService.generateDefaultId();
            }
            newDn = getDnForTelemetryEntry(newInum);
        } while (persistenceEntryManager.contains(newDn, TelemetryEntry.class));
        return newInum;
    }
    
}
