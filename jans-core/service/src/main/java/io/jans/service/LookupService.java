/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.jans.model.DisplayNameEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.Entry;
import io.jans.orm.search.filter.Filter;
import io.jans.util.OxConstants;

/**
 * Provides operations with DisplayNameEntry
 *
 * @author Yuriy Movchan Date: 08/11/2010
 */
@ApplicationScoped
@Named
public class LookupService implements Serializable {

	private static final long serialVersionUID = -3707238475653913313L;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private CacheService cacheService;

	/**
	 * Returns DisplayNameEntry based on display name
	 *
	 * @param dn
	 *            display name
	 * @return DisplayNameEntry object
	 */
	public DisplayNameEntry getDisplayNameEntry(String dn, String objectClass) throws Exception {
		String key = "l_" + objectClass + "_" + dn;
		DisplayNameEntry entry = (DisplayNameEntry) cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
		if (entry == null) {
			// Prepare sample for search
			DisplayNameEntry sample = new DisplayNameEntry();
			sample.setBaseDn(dn);
			sample.setCustomObjectClasses(new String[] { objectClass });

			List<DisplayNameEntry> entries = persistenceEntryManager.findEntries(sample, 1);
			if (entries.size() == 1) {
				entry = entries.get(0);
			}

			cacheService.put(OxConstants.CACHE_LOOKUP_NAME, key, entry);
		}

		return entry;
	}

	public <T> T getDisplayNameEntry(String dn, Class<T> entryClass) throws Exception {
		String key = "l_" + entryClass.getSimpleName() + "_" + dn;
		T entry = (T) cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
		if (entry == null) {
			entry = persistenceEntryManager.find(dn, entryClass, null);

			cacheService.put(OxConstants.CACHE_LOOKUP_NAME, key, entry);
		}

		return entry;
	}

	public DisplayNameEntry getDisplayNameEntry(String dn) throws Exception {
		return getDisplayNameEntry(dn, DisplayNameEntry.class);
	}

	/**
	 * Returns DisplayNameEntry based on display name
	 *
	 * @param dn
	 *            display name
	 * @return DisplayNameEntry object
	 */
	public Object getTypedEntry(String dn, String clazz) throws Exception {
		if (StringUtils.isEmpty(clazz)) {
			return null;
		}

		Class entryClass = Class.class.forName(clazz);
		String key = "l_" + entryClass.getSimpleName() + "_" + dn;
		Object entry = cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
		if (entry == null) {
			entry = persistenceEntryManager.find(entryClass, dn);

			cacheService.put(OxConstants.CACHE_LOOKUP_NAME, key, entry);
		}

		return entry;
	}

	/**
	 * Returns list of DisplayNameEntry objects
	 *
	 * @param baseDn
	 *            base DN
	 * @param dns
	 *            list of display names to find
	 * @return list of DisplayNameEntry objects
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getDisplayNameEntries(String baseDn, Class<T> entryClass, List<String> dns) {
		List<String> inums = getInumsFromDns(dns);
		if (inums.size() == 0) {
			return null;
		}

		String key = getCompoundKey(entryClass, inums);
		List<T> entries = (List<T>) cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
		if (entries == null) {
			Filter searchFilter = buildInumFilter(inums);
			entries = persistenceEntryManager.findEntries(baseDn, entryClass, searchFilter);
			cacheService.put(OxConstants.CACHE_LOOKUP_NAME, key, entries);
		}
		return entries;
	}

	public List<DisplayNameEntry> getDisplayNameEntries(String baseDn, List<String> dns) {
		return getDisplayNameEntries(baseDn, DisplayNameEntry.class, dns);
	}

	public Filter buildInumFilter(List<String> inums) {
		List<Filter> inumFilters = new ArrayList<Filter>(inums.size());
		for (String inum : inums) {
			inumFilters.add(Filter.createEqualityFilter(OxConstants.INUM, inum).multiValued(false));
		}
		return Filter.createORFilter(inumFilters);
	}

	public List<String> getInumsFromDns(List<String> dns) {
		List<String> inums = new ArrayList<String>();

		if (dns == null) {
			return inums;
		}

		for (String dn : dns) {
			String inum = getInumFromDn(dn);
			if (inum != null) {
				inums.add(inum);
			}
		}

		Collections.sort(inums);

		return inums;
	}

	private <T> String getCompoundKey(Class<T> entryClass, List<String> inums) {
		StringBuilder compoundKey = new StringBuilder();
		for (String inum : inums) {
			if (compoundKey.length() > 0) {
				compoundKey.append("_");
			} else {
				compoundKey.append("l_" + entryClass.getSimpleName() + "_");
			}
			compoundKey.append(inum);
		}

		return compoundKey.toString();
	}

	public List<DisplayNameEntry> getDisplayNameEntriesByEntries(String baseDn, List<? extends Entry> entries)
			throws Exception {
		if (entries == null) {
			return null;
		}

		Class objectClass = DisplayNameEntry.class;
		List<String> dns = new ArrayList<String>(entries.size());
		for (Entry entry : entries) {
			dns.add(entry.getDn());
			objectClass = objectClass.getClass();
		}
		

		return getDisplayNameEntries(baseDn, objectClass, dns);
	}

	/**
	 * Get inum from DN
	 *
	 * @param dn
	 *            DN
	 * @return Inum
	 */
	public String getInumFromDn(String dn) {
		if (dn == null) {
			return null;
		}

		if (!dn.startsWith("inum=")) {
			return null;
		}

		int idx = dn.indexOf(",", 5);
		if (idx == -1) {
			return null;
		}

		return dn.substring(5, idx);
	}

}
