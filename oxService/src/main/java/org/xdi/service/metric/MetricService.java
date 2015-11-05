/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.metric;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.model.ApplicationType;
import org.xdi.model.metric.MetricType;
import org.xdi.model.metric.ldap.MetricEntry;
import org.xdi.util.StringHelper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.unboundid.ldap.sdk.Filter;

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
	
	private static final AtomicLong initialId = new AtomicLong(System.currentTimeMillis());

	private MetricRegistry metricRegistry;

	private Set<MetricType> registeredMetricTypes;

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

    public void init(int metricInterval) {
    	this.metricRegistry = new MetricRegistry();
    	this.registeredMetricTypes = new HashSet<MetricType>();
    	
    	LdapEntryReporter ldapEntryReporter = LdapEntryReporter.forRegistry(this.metricRegistry, getComponentName()).build();
    	
    	int metricReporterInterval = metricInterval;
    	if (metricReporterInterval <= 0) {
    		metricReporterInterval = DEFAULT_METRIC_REPORTER_INTERVAL;
    	}
    	ldapEntryReporter.start(metricReporterInterval, TimeUnit.SECONDS);
    }

	@Observer(EVENT_TYPE)
	@Asynchronous
	public void writeMetricEntries(List<MetricEntry> metricEntries, Date creationTime) {
    	add(metricEntries, creationTime);
    }

	public void addBranch(String branchDn, String ou) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName(ou);
		branch.setDn(branchDn);

		ldapEntryManager.persist(branch);
	}

	public boolean containsBranch(String branchDn) {
		return ldapEntryManager.contains(SimpleBranch.class, branchDn);
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
		String baseDn = buildDn(null, creationDate, applicationType);
		// Create ou=YYYY-MM branch if needed
		if (!containsBranch(baseDn)) {
			// Create ou=application_type branch if needed
			String applicationBaseDn = buildDn(null, null, applicationType);
			if (!containsBranch(applicationBaseDn)) {
				// Create ou=appliance_inum branch if needed
				String applianceBaseDn = buildDn(null, null, null);
				if (!containsBranch(applianceBaseDn)) {
					createBranch(applianceBaseDn, applianceInum());
				}

				createBranch(applicationBaseDn, applicationType.getValue());
			}

			if (creationDate != null) {
				createBranch(baseDn, PERIOD_DATE_FORMAT.format(creationDate));
			}
		}
	}

	@Asynchronous
	public void add(List<MetricEntry> metricEntries, Date creationTime) {
		prepareBranch(creationTime, ApplicationType.OX_AUTH);
		
		for (MetricEntry metricEntry : metricEntries) {
			ldapEntryManager.persist(metricEntry);
		}
	}

	public void add(MetricEntry metricEntry) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

		ldapEntryManager.persist(metricEntry);
	}

	public void update(MetricEntry metricEntry) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

		ldapEntryManager.merge(metricEntry);
	}

	public void remove(MetricEntry metricEntry) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType());

		ldapEntryManager.remove(metricEntry);
	}

	public void removeBranch(String branchDn) {
		ldapEntryManager.removeWithSubtree(branchDn);
	}

	public MetricEntry getMetricEntryByDn(MetricType metricType, String metricEventDn) {
		return ldapEntryManager.find(metricType.getMetricEntryType(), metricEventDn);
	}

	public Map<MetricType, List<? extends MetricEntry>> findMetricEntry(ApplicationType applicationType, String applianceInum,
			List<MetricType> metricTypes, Date startDate, Date endDate, String... returnAttributes) {
		prepareBranch(null, applicationType);

		Map<MetricType, List<? extends MetricEntry>> result = new HashMap<MetricType, List<? extends MetricEntry>>();

		if ((metricTypes == null) || (metricTypes.size() == 0)) {
			return result;
		}

		// Prepare list of DNs
		Set<String> metricDns = getBaseDnForPeriod(applicationType, applianceInum, startDate, endDate);
		
		if (metricDns.size() == 0) {
			return result;
		}

		for (MetricType metricType : metricTypes) {
			List<MetricEntry> metricTypeResult = new LinkedList<MetricEntry>();
			for (String metricDn : metricDns) {
				List<Filter> metricTypeFilters = new ArrayList<Filter>();
	
				Filter applicationTypeFilter = Filter.createEqualityFilter("oxApplicationType", applicationType.getValue());
				Filter eventTypeTypeFilter = Filter.createEqualityFilter("oxMetricType", metricType.getValue());
				Filter startDateFilter = Filter.createGreaterOrEqualFilter("oxStartDate", ldapEntryManager.encodeGeneralizedTime((startDate)));
				Filter endDateFilter = Filter.createLessOrEqualFilter("oxEndDate", ldapEntryManager.encodeGeneralizedTime(endDate));
	
				metricTypeFilters.add(applicationTypeFilter);
				metricTypeFilters.add(eventTypeTypeFilter);
				metricTypeFilters.add(startDateFilter);
				metricTypeFilters.add(endDateFilter);
	
				Filter filter = Filter.createANDFilter(metricTypeFilters);
	
				List<? extends MetricEntry> metricTypeMonthResult = (List<? extends MetricEntry>) ldapEntryManager.findEntries(metricDn,
						metricType.getMetricEntryType(), returnAttributes, filter);
				metricTypeResult.addAll(metricTypeMonthResult);
			}
			// Sort entries to avoid calculation errors
			ldapEntryManager.sortListByProperties(MetricEntry.class, metricTypeResult, "creationDate");

			result.put(metricType, metricTypeResult);
		}

		return result;
	}

	public List<MetricEntry> getExpiredMetricEntries(String baseDnForPeriod, Date expirationDate) {
		Filter expiratioFilter = Filter.createLessOrEqualFilter("oxStartDate", ldapEntryManager.encodeGeneralizedTime(expirationDate));

		List<MetricEntry> metricEntries = ldapEntryManager.findEntries(baseDnForPeriod, MetricEntry.class, new String[] { "uniqueIdentifier" }, expiratioFilter);

		return metricEntries;
	}

	public Set<String> findAllPeriodBranches(ApplicationType applicationType, String applianceInum) {
		String baseDn = buildDn(null, null, applicationType, applianceInum);

		Filter skipRootDnFilter = Filter.createNOTFilter(Filter.createEqualityFilter("ou", applicationType.getValue()));
		List<SimpleBranch> periodBranches = (List<SimpleBranch>) ldapEntryManager.findEntries(baseDn, SimpleBranch.class, new String[] { "ou" }, skipRootDnFilter);

		Set<String> periodBranchesStrings = new HashSet<String>();
		for (SimpleBranch periodBranch: periodBranches) {
			if (!StringHelper.equalsIgnoreCase(baseDn, periodBranch.getDn())) {
				periodBranchesStrings.add(periodBranch.getDn());
			}
		}

		return periodBranchesStrings;
	}

	public void removeExpiredMetricEntries(Date expirationDate, ApplicationType applicationType, String applianceInum) {
		Set<String> keepBaseDnForPeriod = getBaseDnForPeriod(applicationType, applianceInum, expirationDate, new Date());
		
		Set<String> allBaseDnForPeriod = findAllPeriodBranches(applicationType, applianceInum);
		
		allBaseDnForPeriod.removeAll(keepBaseDnForPeriod);
		
		// Remove expired months
		for (String baseDnForPeriod : allBaseDnForPeriod) {
			removeBranch(baseDnForPeriod);
		}
		
		// Remove expired entries
		for (String baseDnForPeriod : keepBaseDnForPeriod) {
			List<MetricEntry> expiredMetricEntries = getExpiredMetricEntries(baseDnForPeriod, expirationDate);
			for (MetricEntry expiredMetricEntry : expiredMetricEntries) {
				remove(expiredMetricEntry);
			}
		}
	}

	private Set<String> getBaseDnForPeriod(ApplicationType applicationType, String applianceInum, Date startDate, Date endDate) {
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
		while (true) {  // Add at least one month if the data exists
			int currentMonth = cal.get(Calendar.MONTH);
			Date currentStartDate = cal.getTime();

			String baseDn = buildDn(null, currentStartDate, applicationType, applianceInum);
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
	
	public String getuUiqueIdentifier() {
		return String.valueOf(initialId.incrementAndGet());
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

	public String buildDn(String uniqueIdentifier, Date creationDate, ApplicationType applicationType) {
		return buildDn(uniqueIdentifier, creationDate, applicationType, null);
	}

	/*
	 * Should return similar to this pattern DN:
	 * uniqueIdentifier=id,ou=YYYY-MM,ou=application_type,ou=appliance_inum,ou=metric,ou=organization_name,o=gluu
	 */
	public String buildDn(String uniqueIdentifier, Date creationDate, ApplicationType applicationType, String currentApplianceInum) {
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
		
		if (currentApplianceInum == null) {
			dn.append(String.format("ou=%s,", applianceInum()));
		} else {
			dn.append(String.format("ou=%s,", currentApplianceInum));
		}
		dn.append(baseDn());

		return dn.toString();
	}

	public Set<MetricType> getRegisteredMetricTypes() {
		return registeredMetricTypes;
	}

	// Should return ou=metric,o=gluu
	public abstract String baseDn();

	// Should return appliance Inum
	public abstract String applianceInum();

	public abstract String getComponentName();

	/**
	 * Get MetricService instance
	 *
	 * @return MetricService instance
	 */
	public static MetricService instance() {
		if (!(Contexts.isEventContextActive() || Contexts.isApplicationContextActive())) {
			Lifecycle.beginCall();
		}

		return (MetricService) Component.getInstance(MetricService.class);
	}

}
