/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.model.metric.MetricAggregationType;
import io.jans.model.metric.ldap.MetricAggregationEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read-only access to {@code jansMetricAggregation} entries: pre-computed HOURLY/DAILY/WEEKLY/
 * MONTHLY aggregates of jansMetric data. The producer that writes these rows runs on application
 * nodes and is implemented separately; this service only reads, so it returns an empty page until
 * a producer exists.
 */
@ApplicationScoped
public class MetricAggregationService {

    private static final String AGGREGATION_OU = "aggregation";

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    /**
     * Base DN under which jansMetricAggregation entries are stored (ou=aggregation,ou=statistic,o=metric).
     */
    public String getAggregationBaseDn() {
        return "ou=" + AGGREGATION_OU + "," + staticConfiguration.getBaseDn().getMetric();
    }

    /**
     * Finds a page of jansMetricAggregation entries for an aggregation type within a date range.
     *
     * @param aggregationType required HOURLY/DAILY/WEEKLY/MONTHLY
     * @param appType         optional jansAppTyp filter
     * @param metricType      optional jansMetricTyp filter
     * @param subType         optional jansMetricSubTyp filter
     * @param startDate       inclusive lower bound on jansStartDate
     * @param endDate         inclusive upper bound on jansEndDate
     */
    public PagedResult<MetricAggregationEntry> findEntries(MetricAggregationType aggregationType, String appType,
            String metricType, String subType, Date startDate, Date endDate, String sortBy, SortOrder sortOrder,
            int startIndex, int count, int maxCount) {

        String baseDn = getAggregationBaseDn();

        // The ou=aggregation branch is created by setup on LDAP installs, but existing (upgraded)
        // deployments or backends without branch support (SQL - hasBranchesSupport is false there,
        // so this check is a no-op) may not have it yet. Treat a missing branch as "no data" rather
        // than letting the search fail with a no-such-object error.
        if (persistenceEntryManager.hasBranchesSupport(baseDn)
                && !persistenceEntryManager.contains(baseDn, SimpleBranch.class)) {
            PagedResult<MetricAggregationEntry> empty = new PagedResult<>();
            empty.setStart(startIndex);
            empty.setEntriesCount(0);
            empty.setTotalEntriesCount(0);
            empty.setEntries(new ArrayList<>());
            return empty;
        }

        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.createEqualityFilter("jansAggregationType", aggregationType.getValue()));
        if (appType != null) {
            filters.add(Filter.createEqualityFilter("jansAppTyp", appType));
        }
        if (metricType != null) {
            filters.add(Filter.createEqualityFilter("jansMetricTyp", metricType));
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
        Filter filter = Filter.createANDFilter(filters);

        if (log.isDebugEnabled()) {
            log.debug("Searching jansMetricAggregation entries - baseDn:{}, filter:{}, sortBy:{}, sortOrder:{}",
                    baseDn, filter, sortBy, sortOrder);
        }

        return persistenceEntryManager.findPagedEntries(baseDn, MetricAggregationEntry.class, filter, null, sortBy,
                sortOrder, startIndex, count, maxCount);
    }

}
