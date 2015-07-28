/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.statistic;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.model.ApplicationType;
import org.xdi.model.custom.script.model.CustomScript;
import org.xdi.model.statistic.StatisticEntry;
import org.xdi.model.statistic.StatisticEventType;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;

/**
 * Statistic service
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public abstract class StatisticService implements Serializable {

	private static final long serialVersionUID = -3393618600428448743L;

	private static final SimpleDateFormat PERIOD_DATE_FORMAT = new SimpleDateFormat("yyyyMM");

	@Logger
	private Log log;

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

	@In
	private LdapEntryManager ldapEntryManager;

	public void add(StatisticEntry statisticEntry, String applianceInum) {
		prepareBranch(statisticEntry.getCreationDate(), statisticEntry.getApplicationType(), applianceInum);

		ldapEntryManager.persist(statisticEntry);
	}

	public void update(StatisticEntry statisticEntry, String applianceInum) {
		prepareBranch(statisticEntry.getCreationDate(), statisticEntry.getApplicationType(), applianceInum);

		ldapEntryManager.merge(statisticEntry);
	}

	public void remove(StatisticEntry statisticEntry, String applianceInum) {
		prepareBranch(statisticEntry.getCreationDate(), statisticEntry.getApplicationType(), applianceInum);

		ldapEntryManager.remove(statisticEntry);
	}

	public StatisticEntry getStatisticEventByDn(StatisticEventType statisticEventType, String statisticEventDn) {
		return ldapEntryManager.find(statisticEventType.getStatisticEntryType(), statisticEventDn);
	}

	public Map<StatisticEventType, List<StatisticEntry>> findStatisticEntry(ApplicationType applicationType, String applianceInum,
			List<StatisticEventType> statisticEventTypes, Date startDate, Date endDate, String... returnAttributes) {
		prepareBranch(null, applicationType, applianceInum);

		Map<StatisticEventType, List<StatisticEntry>> result = new HashMap<StatisticEventType, List<StatisticEntry>>();

		if ((statisticEventTypes == null) || (statisticEventTypes.size() == 0)) {
			return result;
		}

		String baseDn = buildDn(applianceInum, applicationType);

		for (StatisticEventType statisticEventType : statisticEventTypes) {
			List<Filter> statisticEventTypeFilters = new ArrayList<Filter>();

			Filter applicationTypeFilter = Filter.createEqualityFilter("oxApplicationType", applicationType.getValue());
			Filter eventTypeTypeFilter = Filter.createEqualityFilter("oxEventType", statisticEventType.getValue());
			statisticEventTypeFilters.add(applicationTypeFilter);
			statisticEventTypeFilters.add(eventTypeTypeFilter);

			Filter filter = Filter.createANDFilter(statisticEventTypeFilters);

			List<StatisticEntry> statisticEventTypeResult = (List<StatisticEntry>) ldapEntryManager.findEntries(baseDn,
					statisticEventType.getStatisticEntryType(), returnAttributes, filter);
			result.put(statisticEventType, statisticEventTypeResult);
		}

		return result;
	}

	public String buildDn(String applianceInum, ApplicationType applicationType) {
		return buildDn(null, null, applicationType, applianceInum);
	}

	/*
	 * Should return similar to this pattern DN:
	 * uniqueIdentifier=id,ou=YYYY-MM,ou
	 * =application_type,ou=appliance_inum,ou=statistic,o=gluu
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

	// Should return ou=statistic,o=gluu
	public abstract String baseDn();

	/**
	 * Get StatisticService instance
	 *
	 * @return StatisticService instance
	 */
	public static StatisticService instance() {
		if (!(Contexts.isEventContextActive() || Contexts.isApplicationContextActive())) {
			Lifecycle.beginCall();
		}

		return (StatisticService) Component.getInstance(StatisticService.class);
	}

}
