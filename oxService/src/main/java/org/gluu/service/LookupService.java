/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.model.DisplayNameEntry;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.Entry;
import org.gluu.search.filter.Filter;
import org.gluu.util.OxConstants;
import org.slf4j.Logger;

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
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private CacheService cacheService;

    /**
     * Returns DisplayNameEntry based on display name
     *
     * @param dn
     *            display name
     * @return DisplayNameEntry object
     */
    public DisplayNameEntry getDisplayNameEntry(String dn) throws Exception {
        String key = dn;
        DisplayNameEntry entry = (DisplayNameEntry) cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
        if (entry == null) {
            entry = ldapEntryManager.find(DisplayNameEntry.class, key, null);

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
    public List<DisplayNameEntry> getDisplayNameEntries(String baseDn, List<String> dns) {
        List<String> inums = getInumsFromDns(dns);
        if (inums.size() == 0) {
            return null;
        }

        String key = getCompoundKey(inums);
        List<DisplayNameEntry> entries = (List<DisplayNameEntry>) cacheService.get(OxConstants.CACHE_LOOKUP_NAME, key);
        if (entries == null) {
            Filter searchFilter = buildInumFilter(inums);

            entries = ldapEntryManager.findEntries(baseDn, DisplayNameEntry.class, searchFilter);

            cacheService.put(OxConstants.CACHE_LOOKUP_NAME, key, entries);
        }

        return entries;
    }

    public Filter buildInumFilter(List<String> inums) {
        List<Filter> inumFilters = new ArrayList<Filter>(inums.size());
        for (String inum : inums) {
            inumFilters.add(Filter.createEqualityFilter(OxConstants.INUM, inum));
        }

        Filter searchFilter = Filter.createORFilter(inumFilters);

        return searchFilter;
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

    private String getCompoundKey(List<String> inums) {
        StringBuilder compoundKey = new StringBuilder();
        for (String inum : inums) {
            if (compoundKey.length() > 0) {
                compoundKey.append("_");
            }
            compoundKey.append(inum);
        }

        return compoundKey.toString();
    }

    public List<DisplayNameEntry> getDisplayNameEntriesByEntries(String baseDn, List<? extends Entry> entries) throws Exception {
        if (entries == null) {
            return null;
        }

        List<String> dns = new ArrayList<String>(entries.size());
        for (Entry entry : entries) {
            dns.add(entry.getDn());
        }

        return getDisplayNameEntries(baseDn, dns);
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
