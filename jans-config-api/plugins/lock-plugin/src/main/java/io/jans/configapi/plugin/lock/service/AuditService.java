package io.jans.configapi.plugin.lock.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;


@ApplicationScoped
public class AuditService {

    @Inject
    Logger logger;
    

    public static final String EVENT_TIME = "eventTime";

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    OrganizationService organizationService;

    @Inject
    private InumService inumService;

    public TelemetryEntry addTelemetryData(TelemetryEntry telemetryEntry) {
        if (telemetryEntry == null) {
            return telemetryEntry;
        }
        String inum = telemetryEntry.getInum();
        if (StringUtils.isBlank(inum)) {
            inum = this.generateInumForEntry("telemetry", TelemetryEntry.class);
            telemetryEntry.setInum(inum);

            telemetryEntry.setDn(this.getDnForTelemetryEntry(inum));
        }
        persistenceEntryManager.persist(telemetryEntry);

        telemetryEntry = this.getTelemetryEntryByDn(this.getDnForTelemetryEntry(inum));
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
        return persistenceEntryManager.findEntries(getDnForTelemetryEntry(null), TelemetryEntry.class, searchFilter,
                sizeLimit);
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

        return persistenceEntryManager.findPagedEntries(getDnForTelemetryEntry(null), TelemetryEntry.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public List<TelemetryEntry> getTelemetryEntrysByRange(Date eventStartDate, Date eventEndDate, int sizeLimit) {
        logger.debug("Search TelemetryEntrys by event range: [{}, {}], sizeLimit:{}", eventStartDate, eventEndDate, sizeLimit);

        String baseDn = getDnForTelemetryEntry(null);
        
        Filter eventStartDateFilter = Filter.createGreaterOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventStartDate));
        Filter eventEndDateFilter = Filter.createLessOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventEndDate));
        
        Filter searchFilter = Filter.createANDFilter(eventStartDateFilter, eventEndDateFilter);

        logger.debug("Search TelemetryEntrys with searchFilter: {}", searchFilter);
		return persistenceEntryManager.findEntries(baseDn, TelemetryEntry.class, searchFilter,
                sizeLimit);
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
            return String.format("ou=telemetry,ou=lock,%s", orgDn);
        }
        return String.format("inum=%s,ou=telemetry,ou=lock,%s", inum, orgDn);
    }

    public HealthEntry addHealthEntry(HealthEntry healthEntry) {
        if (healthEntry == null) {
            return healthEntry;
        }
        String inum = healthEntry.getInum();
        if (StringUtils.isBlank(inum)) {
            inum = this.generateInumForEntry("health",HealthEntry.class);
            healthEntry.setInum(inum);

            healthEntry.setDn(this.getDnForHealthEntry(inum));
        }
        persistenceEntryManager.persist(healthEntry);

        healthEntry = this.getHealthEntryByDn(this.getDnForHealthEntry(inum));
        return healthEntry;
    }

    public List<HealthEntry> getHealthEntrysByRange(Date eventDateStart, Date eventDateEnd, int sizeLimit) {
        logger.debug("Search HealthEntrys by event range: [{}, {}], sizeLimit:{}", eventDateStart, eventDateEnd, sizeLimit);

        String baseDn = getDnForHealthEntry(null);
        
        Filter eventDateStartFilter = Filter.createGreaterOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventDateStart));
        Filter eventDateEndFilter = Filter.createLessOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventDateEnd));
        
        Filter searchFilter = Filter.createANDFilter(eventDateStartFilter, eventDateEndFilter);

        logger.debug("Search HealthEntrys with searchFilter: {}", searchFilter);
		return persistenceEntryManager.findEntries(baseDn, HealthEntry.class, searchFilter,
                sizeLimit);
    }

    public HealthEntry getHealthEntryByDn(String dn) {
        try {
            return persistenceEntryManager.find(HealthEntry.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public String getDnForHealthEntry(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=health,ou=lock,%s", orgDn);
        }
        return String.format("inum=%s,ou=health,ou=lock,%s", inum, orgDn);
    }

    public LogEntry addLogData(LogEntry logEntry) {
        if (logEntry == null) {
            return logEntry;
        }
        String inum = logEntry.getInum();
        if (StringUtils.isBlank(inum)) {
            inum = this.generateInumForEntry("log", LogEntry.class);
            logEntry.setInum(inum);

            logEntry.setDn(this.getDnForLogEntry(inum));
        }
        persistenceEntryManager.persist(logEntry);

        logEntry = this.getLogEntryByDn(this.getDnForLogEntry(inum));
        return logEntry;
    }

    public List<LogEntry> getLogEntrysByRange(Date eventDateStart, Date eventDateEnd, int sizeLimit) {
        logger.debug("Search LogEntrys by event range: [{}, {}], sizeLimit:{}", eventDateStart, eventDateEnd, sizeLimit);

        String baseDn = getDnForLogEntry(null);
        
        Filter eventDateStartFilter = Filter.createGreaterOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventDateStart));
        Filter eventDateEndFilter = Filter.createLessOrEqualFilter(EVENT_TIME, persistenceEntryManager.encodeTime(baseDn, eventDateEnd));
        
        Filter searchFilter = Filter.createANDFilter(eventDateStartFilter, eventDateEndFilter);

        logger.debug("Search LogEntrys with searchFilter: {}", searchFilter);
        return persistenceEntryManager.findEntries(baseDn, LogEntry.class, searchFilter,
                sizeLimit);
    }
   
    public LogEntry getLogEntryByDn(String dn) {
        try {
            return persistenceEntryManager.find(LogEntry.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }

    public String getDnForLogEntry(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=log,ou=lock,%s", orgDn);
        }
        return String.format("inum=%s,ou=log,ou=lock,%s", inum, orgDn);
    }
    
    public String generateInumForEntry(String entryName, Class classObj) {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < InumService.MAX_IDGEN_TRY_COUNT) {
                newInum = inumService.generateId(entryName);
                trycount++;
            } else {
                newInum = inumService.generateDefaultId();
            }
            newDn = getDnForLogEntry(newInum);
        } while (persistenceEntryManager.contains(newDn, classObj));
        return newInum;
    }
    
   

}
