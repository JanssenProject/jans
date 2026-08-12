/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.plugin.metric.model.MetricTypeInfo;
import io.jans.model.metric.ldap.MetricDataEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Read-only access to {@code jansMetric} entries written by application nodes (jans-auth,
 * fido2, jans-lock, ...) via io.jans.service.metric.MetricService.
 *
 * Queries are issued once against the application-level metric base DN with the date range
 * folded into the filter, rather than one query per monthly branch as MetricService.findMetricEntry
 * does: on RDBM persistence a base DN only resolves the table name (there is no DN-scoping in the
 * generated SQL), so repeating the same query per month returns every matching row once per month.
 */
@ApplicationScoped
public class MetricDataService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    /**
     * Base DN under which jansMetric entries are stored (ou=statistic,o=metric).
     */
    public String getMetricBaseDn() {
        return staticConfiguration.getBaseDn().getMetric();
    }

    /**
     * Finds a page of jansMetric entries for a metric type within a date range.
     *
     * @param appType    optional jansAppTyp filter
     * @param metricType required jansMetricTyp filter
     * @param subType    optional jansMetricSubTyp filter; when null, both plain and per-subtype
     *                   rows are returned (callers must not sum both without accounting for this)
     * @param startDate  inclusive lower bound on jansStartDate
     * @param endDate    inclusive upper bound on jansEndDate
     * @param sortBy     attribute to sort by (caller must have whitelisted this)
     * @param sortOrder  ascending or descending
     * @param startIndex 0-based offset of the first result to return
     * @param count      page size
     * @param maxCount   maximum records the persistence layer may fetch internally
     */
    public PagedResult<MetricDataEntry> findEntries(String appType, String metricType, String subType, Date startDate,
            Date endDate, String sortBy, SortOrder sortOrder, int startIndex, int count, int maxCount) {

        String baseDn = getMetricBaseDn();
        Filter filter = buildDataFilter(baseDn, appType, metricType, subType, startDate, endDate);

        if (log.isDebugEnabled()) {
            log.debug("Searching jansMetric entries - baseDn:{}, filter:{}, sortBy:{}, sortOrder:{}", baseDn, filter,
                    sortBy, sortOrder);
        }

        return persistenceEntryManager.findPagedEntries(baseDn, MetricDataEntry.class, filter, null, sortBy,
                sortOrder, startIndex, count, maxCount);
    }

    /**
     * Discovers the distinct (applicationType, metricType, metricSubType) combinations present in
     * jansMetric, optionally narrowed by appType and/or a date range. Prefers the persistence
     * backend's server-side SELECT DISTINCT ({@link #findDistinctRows}); on a backend without it,
     * falls back to scanning up to {@code scanLimit} matching rows (with only the three attributes
     * needed) and deduping in memory.
     */
    public List<MetricTypeInfo> findMetricTypes(String appType, Date startDate, Date endDate, int scanLimit) {
        String baseDn = getMetricBaseDn();
        Filter filter = buildDiscoveryFilter(baseDn, appType, startDate, endDate);
        String[] returnAttributes = { "jansAppTyp", "jansMetricTyp", "jansMetricSubTyp" };

        List<MetricDataEntry> rows = findDistinctRows(baseDn, filter, returnAttributes, scanLimit);

        if (log.isDebugEnabled()) {
            log.debug("Discovered {} jansMetric rows while resolving distinct metric types (scanLimit:{})",
                    rows == null ? 0 : rows.size(), scanLimit);
        }

        Map<String, Map<String, MetricTypeInfo>> byAppThenType = new TreeMap<>();
        if (rows != null) {
            for (MetricDataEntry row : rows) {
                String app = row.getApplicationType();
                String type = row.getMetricType();
                if (type == null) {
                    continue;
                }
                // TreeMap rejects a null key; jansAppTyp is optional in the schema, so a row missing
                // it must not crash discovery - bucket it under "" rather than skipping the row.
                String appKey = app == null ? "" : app;
                Map<String, MetricTypeInfo> byType = byAppThenType.computeIfAbsent(appKey, k -> new TreeMap<>());
                MetricTypeInfo info = byType.computeIfAbsent(type, k -> new MetricTypeInfo(app, type));
                if (row.getMetricSubType() != null) {
                    info.getSubTypes().add(row.getMetricSubType());
                }
            }
        }

        List<MetricTypeInfo> result = new ArrayList<>();
        for (Map<String, MetricTypeInfo> byType : byAppThenType.values()) {
            result.addAll(byType.values());
        }
        return result;
    }

    /**
     * Discovers the distinct jansAppTyp values present in jansMetric.
     */
    public List<String> findAppTypes(int scanLimit) {
        String baseDn = getMetricBaseDn();
        String[] returnAttributes = { "jansAppTyp" };

        List<MetricDataEntry> rows = findDistinctRows(baseDn, null, returnAttributes, scanLimit);

        Set<String> appTypes = new TreeSet<>();
        if (rows != null) {
            for (MetricDataEntry row : rows) {
                if (row.getApplicationType() != null) {
                    appTypes.add(row.getApplicationType());
                }
            }
        }
        return new ArrayList<>(appTypes);
    }

    /**
     * SELECT DISTINCT over {@code attributes}, mapped to partial {@code MetricDataEntry} beans (DN
     * unset). Only the SQL backend (and hybrid when routed to SQL) implements server-side DISTINCT
     * today - LDAP, Couchbase and Spanner fall through the ORM's default method, which throws
     * {@link UnsupportedOperationException}; in that case this scans up to {@code scanLimit} raw
     * rows instead, leaving deduplication to the caller.
     */
    private List<MetricDataEntry> findDistinctRows(String baseDn, Filter filter, String[] attributes,
            int scanLimit) {
        try {
            SearchProjection projection = SearchProjection.distinct(attributes);
            return persistenceEntryManager.findDistinctEntries(baseDn, MetricDataEntry.class, filter, projection, 0,
                    scanLimit);
        } catch (UnsupportedOperationException e) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Server-side DISTINCT not supported by '{}' persistence backend, falling back to scanning up to {} rows",
                        persistenceEntryManager.getPersistenceType(), scanLimit);
            }
            return persistenceEntryManager.findEntries(baseDn, MetricDataEntry.class, filter, attributes, scanLimit);
        }
    }

    private Filter buildDataFilter(String baseDn, String appType, String metricType, String subType, Date startDate,
            Date endDate) {
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.createEqualityFilter("jansMetricTyp", metricType));
        if (appType != null) {
            filters.add(Filter.createEqualityFilter("jansAppTyp", appType));
        }
        if (subType != null) {
            filters.add(Filter.createEqualityFilter("jansMetricSubTyp", subType));
        }
        if (startDate != null) {
            filters.add(Filter.createGreaterOrEqualFilter("jansStartDate",
                    persistenceEntryManager.encodeTime(baseDn, startDate)));
        }
        if (endDate != null) {
            filters.add(
                    Filter.createLessOrEqualFilter("jansEndDate", persistenceEntryManager.encodeTime(baseDn, endDate)));
        }
        return Filter.createANDFilter(filters);
    }

    private Filter buildDiscoveryFilter(String baseDn, String appType, Date startDate, Date endDate) {
        List<Filter> filters = new ArrayList<>();
        if (appType != null) {
            filters.add(Filter.createEqualityFilter("jansAppTyp", appType));
        }
        if (startDate != null) {
            filters.add(Filter.createGreaterOrEqualFilter("jansStartDate",
                    persistenceEntryManager.encodeTime(baseDn, startDate)));
        }
        if (endDate != null) {
            filters.add(
                    Filter.createLessOrEqualFilter("jansEndDate", persistenceEntryManager.encodeTime(baseDn, endDate)));
        }
        if (filters.isEmpty()) {
            return null;
        }
        return Filter.createANDFilter(filters);
    }

}
