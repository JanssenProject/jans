/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.jans.model.ApplicationType;
import io.jans.model.metric.MetricType;
import io.jans.model.metric.ldap.MetricEntry;
import io.jans.model.metric.ldap.MetricReport;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.DefaultBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metric service
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public abstract class MetricService implements Serializable {

    private static final long serialVersionUID = -3393618600428448743L;

    private static final String EVENT_TYPE = "MetricServiceTimerEvent";

    private static final int DEFAULT_METRIC_REPORTER_INTERVAL = 60;

    private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");

    private static final AtomicLong INITIAL_ID = new AtomicLong(System.currentTimeMillis());

    private MetricRegistry metricRegistry;

    private Set<MetricType> registeredMetricTypes;

    private int entryLifetimeInDays;

	private LdapEntryReporter ldapEntryReporter;

    @Inject
    private Logger log;

    public void initTimer(int metricInterval, int entryLifetimeInDays) {
        this.metricRegistry = new MetricRegistry();
        this.registeredMetricTypes = new HashSet<MetricType>();
        this.entryLifetimeInDays = entryLifetimeInDays;

        this.ldapEntryReporter = LdapEntryReporter.forRegistry(this.metricRegistry, getMetricServiceInstance()).build();

        int metricReporterInterval = metricInterval;
        if (metricReporterInterval <= 0) {
            metricReporterInterval = DEFAULT_METRIC_REPORTER_INTERVAL;
        }
        ldapEntryReporter.start(metricReporterInterval, TimeUnit.SECONDS);
    }

    public void close() {
    	if (this.ldapEntryReporter != null) {
    		this.ldapEntryReporter.close();
    	}
    }

    public int getEntryLifetimeInDays() {
        return entryLifetimeInDays;
    }

    @Asynchronous
    public void writeMetricEntries(@Observes @ReportMetric MetricReport metricReport) {
        add(metricReport.getMetricEntries(), metricReport.getCreationTime());
    }

    public void addBranch(String branchDn, String ou) {
    	if (getEntryManager().hasBranchesSupport(branchDn)) {
	        SimpleBranch branch = new SimpleBranch();
	        branch.setOrganizationalUnitName(ou);
	        branch.setDn(branchDn);
	
	        getEntryManager().persist(branch);
    	}
    }

    public boolean containsBranch(String branchDn) {
    	if (getEntryManager().hasBranchesSupport(branchDn)) {
            return getEntryManager().contains(branchDn, SimpleBranch.class);
    	} else {
    		return true;
    	}
    }

    public void createBranch(String branchDn, String ou) {
        try {
            addBranch(branchDn, ou);
        } catch (EntryPersistenceException ex) {
            // Check if another process added this branch already
            if (!containsBranch(branchDn)) {
                throw ex;
            }
        }
    }

    public void prepareBranch(Date creationDate, ApplicationType applicationType) {
        if (!getEntryManager().hasBranchesSupport(baseDn())) {
        	return;
        }

        String baseDn = buildDn(null, creationDate, applicationType);
        // Create ou=YYYY-MM branch if needed
        if (!containsBranch(baseDn)) {
            createApplicationBaseBranch(applicationType);

            if (creationDate != null) {
                createBranch(baseDn, PERIOD_DATE_FORMAT.format(creationDate));
            }
        }
    }

    protected void createApplicationBaseBranch(ApplicationType applicationType) {
        // Create ou=application_type branch if needed
        String applicationBaseDn = buildDn(null, null, applicationType);
        if (!containsBranch(applicationBaseDn)) {
        	createBranch(applicationBaseDn, applicationType.getValue());
        }
    }

    public void add(List<MetricEntry> metricEntries, Date creationTime) {
        prepareBranch(creationTime, getApplicationType());

        for (MetricEntry metricEntry : metricEntries) {
            getEntryManager().persist(metricEntry);
        }
    }

    public void add(MetricEntry metricEntry) {
        prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

        getEntryManager().persist(metricEntry);
    }

    public void update(MetricEntry metricEntry) {
        prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

        getEntryManager().merge(metricEntry);
    }

    public void remove(MetricEntry metricEntry) {
        prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

        getEntryManager().remove(metricEntry);
    }

    public void removeBranch(String branchDn) {
        getEntryManager().removeRecursively(branchDn, SimpleBranch.class);
    }

    public MetricEntry getMetricEntryByDn(MetricType metricType, String metricEventDn) {
        return getEntryManager().find(metricType.getMetricEntryType(), metricEventDn);
    }

    public Map<MetricType, List<? extends MetricEntry>> findMetricEntry(ApplicationType applicationType,
            List<MetricType> metricTypes, Date startDate, Date endDate, String... returnAttributes) {
        prepareBranch(null, applicationType);

        Map<MetricType, List<? extends MetricEntry>> result = new HashMap<MetricType, List<? extends MetricEntry>>();

        if ((metricTypes == null) || (metricTypes.size() == 0)) {
            return result;
        }

        // Prepare list of DNs
        Set<String> metricDns = getBaseDnForPeriod(applicationType, startDate, endDate);

        if (metricDns.size() == 0) {
            return result;
        }

        for (MetricType metricType : metricTypes) {
            List<MetricEntry> metricTypeResult = new LinkedList<MetricEntry>();
            for (String metricDn : metricDns) {
                List<Filter> metricTypeFilters = new ArrayList<Filter>();

                Filter applicationTypeFilter = Filter.createEqualityFilter("jansAppTyp", applicationType.getValue());
                Filter eventTypeTypeFilter = Filter.createEqualityFilter("jansMetricTyp", metricType.getValue());
                Filter startDateFilter = Filter.createGreaterOrEqualFilter("jansStartDate", getEntryManager().encodeTime(metricDn, (startDate)));
                Filter endDateFilter = Filter.createLessOrEqualFilter("jansEndDate", getEntryManager().encodeTime(metricDn, endDate));

                metricTypeFilters.add(applicationTypeFilter);
                metricTypeFilters.add(eventTypeTypeFilter);
                metricTypeFilters.add(startDateFilter);
                metricTypeFilters.add(endDateFilter);

                Filter filter = Filter.createANDFilter(metricTypeFilters);

                List<? extends MetricEntry> metricTypeMonthResult = (List<? extends MetricEntry>) getEntryManager().findEntries(metricDn,
                        metricType.getMetricEntryType(), filter, returnAttributes);
                metricTypeResult.addAll(metricTypeMonthResult);
            }
            // Sort entries to avoid calculation errors
            getEntryManager().sortListByProperties(MetricEntry.class, metricTypeResult, false, "creationDate");

            result.put(metricType, metricTypeResult);
        }

        return result;
    }

    public List<MetricEntry> getExpiredMetricEntries(DefaultBatchOperation<MetricEntry> batchOperation, ApplicationType applicationType, String baseDnForPeriod, Date expirationDate, int count, int chunkSize) {
		Filter expiratioStartDateFilter = Filter.createLessOrEqualFilter("oxStartDate", getEntryManager().encodeTime(baseDnForPeriod, expirationDate));

		Filter expiratioFilter = expiratioStartDateFilter;
		if (applicationType != null) {
			Filter applicationTypeFilter = Filter.createEqualityFilter("oxMetricType", applicationType.getValue());
			expiratioFilter = Filter.createANDFilter(expiratioStartDateFilter, applicationTypeFilter);
		}

        List<MetricEntry> metricEntries = getEntryManager().findEntries(baseDnForPeriod, MetricEntry.class, expiratioFilter, SearchScope.SUB,
                new String[] { "uniqueIdentifier" }, batchOperation, 0, count, chunkSize);

        return metricEntries;
    }

    public List<SimpleBranch> findAllPeriodBranches(DefaultBatchOperation<SimpleBranch> batchOperation, ApplicationType applicationType,
            int count, int chunkSize) {
        String baseDn = buildDn(null, null, applicationType);

        Filter skipRootDnFilter = Filter.createNOTFilter(Filter.createEqualityFilter("ou", applicationType.getValue()));
        return getEntryManager().findEntries(baseDn, SimpleBranch.class, skipRootDnFilter, SearchScope.SUB, new String[] { "ou" }, batchOperation, 0,
                count, chunkSize);
    }

    public void removeExpiredMetricEntries(final Date expirationDate, final ApplicationType applicationType, int count,
            int chunkSize) {
        createApplicationBaseBranch(applicationType);

        final Set<String> keepBaseDnForPeriod = getBaseDnForPeriod(applicationType, expirationDate, new Date());
        // Remove expired entries
        for (final String baseDnForPeriod : keepBaseDnForPeriod) {
            DefaultBatchOperation<MetricEntry> metricEntryBatchOperation = new DefaultBatchOperation<MetricEntry>() {
                @Override
                public boolean collectSearchResult(int size) {
                    return false;
                }

                @Override
                public void performAction(List<MetricEntry> entries) {
                    for (MetricEntry metricEntry : entries) {
                        remove(metricEntry);
                    }
                }
            };
            getExpiredMetricEntries(metricEntryBatchOperation, applicationType, baseDnForPeriod, expirationDate, count, chunkSize);
        }

        if (!getEntryManager().hasBranchesSupport(buildDn(null, null, applicationType))) {
	        DefaultBatchOperation<SimpleBranch> batchOperation = new DefaultBatchOperation<SimpleBranch>() {
	            @Override
	            public boolean collectSearchResult(int size) {
	                return false;
	            }
	
	            @Override
	            public void performAction(List<SimpleBranch> objects) {
	                String baseDn = buildDn(null, null, applicationType);
	                Set<String> periodBranchesStrings = new HashSet<String>();
	                for (SimpleBranch periodBranch : objects) {
	                    if (!StringHelper.equalsIgnoreCase(baseDn, periodBranch.getDn())) {
	                        periodBranchesStrings.add(periodBranch.getDn());
	                    }
	                }
	                periodBranchesStrings.removeAll(keepBaseDnForPeriod);
	
	                // Remove expired months
	                for (String baseDnForPeriod : periodBranchesStrings) {
	                    removeBranch(baseDnForPeriod);
	                }
	            }
	        };
	        findAllPeriodBranches(batchOperation, applicationType, count, chunkSize);
	    }
    }

    private Set<String> getBaseDnForPeriod(ApplicationType applicationType, Date startDate, Date endDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(startDate);

        Calendar calEndMonth = Calendar.getInstance();
        calEndMonth.setTimeZone(TimeZone.getTimeZone("UTC"));
        calEndMonth.setTime(endDate);
        int endMonth = calEndMonth.get(Calendar.MONTH);

        Set<String> metricDns = new HashSet<String>();

        boolean stopCondition = cal.getTime().equals(endDate);
        cal.setTime(startDate);
        while (true) { // Add at least one month if the data exists
            int currentMonth = cal.get(Calendar.MONTH);
            Date currentStartDate = cal.getTime();

            String baseDn = buildDn(null, currentStartDate, applicationType);
            if (containsBranch(baseDn)) {
                metricDns.add(baseDn);
            }

            if (stopCondition) {
                break;
            } else {
                cal.add(Calendar.MONTH, 1);

                if (cal.getTime().after(endDate)) { // Stop condition which allows to add DN for end of the period
                    stopCondition = true;
                    if (currentMonth == endMonth) {
                        break;
                    }
                }
            }
        }

        return metricDns;
    }

    public String getUiqueIdentifier() {
        return UUID.randomUUID().toString();
    }

    public Counter getCounter(MetricType metricType) {
        if (!registeredMetricTypes.contains(metricType)) {
            registeredMetricTypes.add(metricType);
        }

        return metricRegistry.counter(metricType.getMetricName());
    }

    public Timer getTimer(MetricType metricType) {
        if (!registeredMetricTypes.contains(metricType)) {
            registeredMetricTypes.add(metricType);
        }

        return metricRegistry.timer(metricType.getMetricName());
    }

    public void incCounter(MetricType metricType) {
        Counter counter = getCounter(metricType);
        counter.inc();
    }

    /*
     * Should return similar to this pattern DN:
     * uniqueIdentifier=id,ou=YYYY-MM,ou=application_type,ou=metric,ou=organization_name,o=jans
     */
    public String buildDn(String uniqueIdentifier, Date creationDate, ApplicationType applicationType) {
        final StringBuilder dn = new StringBuilder();
        if (StringHelper.isNotEmpty(uniqueIdentifier) && (creationDate != null) && (applicationType != null)) {
            dn.append(String.format("uniqueIdentifier=%s,", uniqueIdentifier));
        }
        if ((creationDate != null) && (applicationType != null)) {
            dn.append(String.format("ou=%s,", PERIOD_DATE_FORMAT.format(creationDate)));
        }
        if (applicationType != null) {
            dn.append(String.format("ou=%s,", applicationType.getValue()));
        }

        dn.append(baseDn());

        return dn.toString();
    }

    public Set<MetricType> getRegisteredMetricTypes() {
        return registeredMetricTypes;
    }

    // Should return ou=metric,o=jans
    public abstract String baseDn();

    public abstract MetricService getMetricServiceInstance();

    public abstract boolean isMetricReporterEnabled();

    public abstract ApplicationType getApplicationType();

    public abstract String getNodeIndetifier();

    public abstract PersistenceEntryManager getEntryManager();
}
