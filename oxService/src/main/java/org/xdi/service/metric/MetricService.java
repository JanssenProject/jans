/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.metric;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.model.ApplicationType;
import org.xdi.model.metric.MetricEventType;
import org.xdi.model.metric.counter.CounterMetricData;
import org.xdi.model.metric.counter.CounterMetricEntry;
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

	private final static String EVENT_TYPE = "MetricServiceTimerEvent";

	private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");
	
	private static final AtomicLong initialId = new AtomicLong(System.currentTimeMillis());

	private MetricRegistry metricRegistry;

	private Set<MetricEventType> registeredMetricEventTypes;

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

	private AtomicBoolean isActive;
	private long lastFinishedTime;

    public void init(int metricInterval) {
    	this.metricRegistry = new MetricRegistry();
    	this.registeredMetricEventTypes = new HashSet<MetricEventType>();

		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();

		Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(1 * 60 * 1000L, metricInterval * 1000L));
    }

	@Observer(EVENT_TYPE)
	@Asynchronous
	public void writeMetricTimerEvent() {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			writeMetric();
		} catch (Throwable ex) {
			log.error("Exception happened while writing metric data", ex);
		} finally {
			this.isActive.set(false);
			this.lastFinishedTime = System.currentTimeMillis();
			log.trace("Last finished time '{0}'", new Date(this.lastFinishedTime));
		}
	}

	private void writeMetric() {
		List<MetricEntry> result = new ArrayList<MetricEntry>();
		
		Set<MetricEventType> currentRegisteredMetricEventTypes = new HashSet<MetricEventType>(this.registeredMetricEventTypes);
		for (MetricEventType metricEventType : currentRegisteredMetricEventTypes) {
			if (metricEventType.getEventDataType().equals(CounterMetricData.class)) {
				Counter counter = getCounter(metricEventType);
				if (counter != null) {
					long count = counter.getCount();

					removeCounter(metricEventType);

					CounterMetricData counterMetricData = new CounterMetricData(count);
					CounterMetricEntry counterMetricEntry = new CounterMetricEntry();
					counterMetricEntry.setMetricData(counterMetricData);

					result.add(counterMetricEntry);
				}
			}
		}
		
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!:" + result);
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

	public void prepareBranch(Date creationDate, ApplicationType applicationType, String applianceInum) {
		String baseDn = buildDn(null, creationDate, applicationType, applianceInum);
		// Create ou=YYYY-MM branch if needed
		if (!containsBranch(baseDn)) {
			// Create ou=application_type branch if needed
			String applicationBaseDn = buildDn(null, null, applicationType, applianceInum);
			if (!containsBranch(applicationBaseDn)) {
				// Create ou=appliance_inum branch if needed
				String applianceBaseDn = buildDn(null, null, null, applianceInum);
				if (!containsBranch(applianceBaseDn)) {
					createBranch(applianceBaseDn, applianceInum);
				}

				createBranch(applicationBaseDn, applicationType.getValue());
			}

			createBranch(baseDn, PERIOD_DATE_FORMAT.format(creationDate));
		}
	}

	public void add(MetricEntry metricEntry, String applianceInum) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType(), applianceInum);

		ldapEntryManager.persist(metricEntry);
	}

	public void update(MetricEntry metricEntry, String applianceInum) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType(), applianceInum);

		ldapEntryManager.merge(metricEntry);
	}

	public void remove(MetricEntry metricEntry, String applianceInum) {
		prepareBranch(metricEntry.getCreationDate(), metricEntry.getApplicationType(), applianceInum);

		ldapEntryManager.remove(metricEntry);
	}

	public MetricEntry getMetricEntryByDn(MetricEventType metricEventType, String metricEventDn) {
		return ldapEntryManager.find(metricEventType.getMetricEntryType(), metricEventDn);
	}

	public Map<MetricEventType, List<MetricEntry>> findMetricEntry(ApplicationType applicationType, String applianceInum,
			List<MetricEventType> metricEventTypes, Date startDate, Date endDate, String... returnAttributes) {
		prepareBranch(null, applicationType, applianceInum);

		Map<MetricEventType, List<MetricEntry>> result = new HashMap<MetricEventType, List<MetricEntry>>();

		if ((metricEventTypes == null) || (metricEventTypes.size() == 0)) {
			return result;
		}

		String baseDn = buildDn(applianceInum, applicationType);

		for (MetricEventType metricEventType : metricEventTypes) {
			List<Filter> metricEventTypeFilters = new ArrayList<Filter>();

			Filter applicationTypeFilter = Filter.createEqualityFilter("oxApplicationType", applicationType.getValue());
			Filter eventTypeTypeFilter = Filter.createEqualityFilter("oxEventType", metricEventType.getValue());
			metricEventTypeFilters.add(applicationTypeFilter);
			metricEventTypeFilters.add(eventTypeTypeFilter);

			Filter filter = Filter.createANDFilter(metricEventTypeFilters);

			List<MetricEntry> metricEventTypeResult = (List<MetricEntry>) ldapEntryManager.findEntries(baseDn,
					metricEventType.getMetricEntryType(), returnAttributes, filter);
			result.put(metricEventType, metricEventTypeResult);
		}

		return result;
	}
	
	public String getuUiqueIdentifier() {
		return String.valueOf(initialId.incrementAndGet());
	}

	public Counter getCounter(MetricEventType metricEventType) {
		if (!registeredMetricEventTypes.contains(metricEventType)) {
			registeredMetricEventTypes.add(metricEventType);
		}

		return metricRegistry.counter(metricEventType.getMetricName());
	}

	private void removeCounter(MetricEventType metricEventType) {
		if (registeredMetricEventTypes.contains(metricEventType)) {
			registeredMetricEventTypes.remove(metricEventType);
		}
		metricRegistry.remove(metricEventType.getMetricName());
	}

	public Timer getTimer(MetricEventType metricEventType) {
		if (!registeredMetricEventTypes.contains(metricEventType)) {
			registeredMetricEventTypes.add(metricEventType);
		}

		return metricRegistry.timer(metricEventType.getMetricName());
	}

	public void incCounter(MetricEventType metricEventType) {
		Counter counter = getCounter(metricEventType);
		counter.inc();
	}

	public String buildDn(String applianceInum, ApplicationType applicationType) {
		return buildDn(null, null, applicationType, applianceInum);
	}

	/*
	 * Should return similar to this pattern DN:
	 * uniqueIdentifier=id,ou=YYYY-MM,ou=application_type,ou=appliance_inum,ou=metric,o=gluu
	 */
	public String buildDn(String uniqueIdentifier, Date creationDate, ApplicationType applicationType, String applianceInum) {
		final StringBuilder dn = new StringBuilder();
		if (StringHelper.isNotEmpty(uniqueIdentifier) && (creationDate != null)) {
			dn.append(String.format("uniqueIdentifier=%s,", uniqueIdentifier));
		}
		if (creationDate != null) {
			dn.append(String.format("ou=%s,", PERIOD_DATE_FORMAT.format(creationDate)));
		}
		if (StringHelper.isNotEmpty(uniqueIdentifier) && (creationDate != null) && (applicationType != null)) {
			dn.append(String.format("ou=%s,", applicationType.getValue()));
		}
		dn.append(String.format("ou=%s,", applianceInum));
		dn.append(baseDn());

		return dn.toString();
	}

	// Should return ou=metric,o=gluu
	public abstract String baseDn();

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
