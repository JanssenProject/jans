/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * Server-side GROUP BY with aggregates over jansPerson
 *
 * @author Yuriy Movchan
 */
public final class SqlGroupBySample {

    private static final String DN_PEOPLE = "ou=people,o=jans";
	private static final Logger LOG = LoggerFactory.getLogger(SqlGroupBySample.class);

    private SqlGroupBySample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        try {
            // Count users per status, biggest groups first
            SearchProjection projection = SearchProjection.groupBy("jansStatus").count()
                    .orderBy("total", SortOrder.DESCENDING);

            PagedResult<EntryData> result = sqlEntryManager.findAggregatedEntries(DN_PEOPLE,
                    SimpleUser.class, null, projection, 0, 100);

            LOG.info("Total groups: {}", result.getTotalEntriesCount());
            long groupsSum = 0;
            for (EntryData row : result.getEntries()) {
                Object status = (row.getAttributeData("jansStatus") == null) ? null : row.getAttributeData("jansStatus").getValue();
                Object total = row.getAttributeData("total").getValue();
                LOG.info("jansStatus: {}, total: {}", status, total);
                groupsSum += Long.parseLong(String.valueOf(total));
            }

            // Cross-check: sum of group counts must match countEntries with the same filter
            int allUsers = sqlEntryManager.countEntries(DN_PEOPLE, SimpleUser.class, null);
            LOG.info("Sum of group counts: {}, countEntries: {}", groupsSum, allUsers);

            // Groups filtered by WHERE clause
            Filter filter = Filter.createPresenceFilter("mail");
            PagedResult<EntryData> filtered = sqlEntryManager.findAggregatedEntries(DN_PEOPLE,
                    SimpleUser.class, filter, SearchProjection.groupBy("jansStatus").count(), 0, 100);
            LOG.info("Groups among users with mail: {}", filtered.getTotalEntriesCount());
            for (EntryData row : filtered.getEntries()) {
                LOG.info("Row: {}", row);
            }
        } finally {
            sqlEntryManager.destroy();
        }
    }

}
