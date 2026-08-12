/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * Server-side SELECT DISTINCT over jansPerson
 *
 * @author Yuriy Movchan Date: 08/12/2025
 */
public final class SqlDistinctSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlDistinctSample.class);

    private SqlDistinctSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        try {
            // Distinct raw rows: unique status values
            PagedResult<EntryData> distinctRows = sqlEntryManager.findAggregatedEntries("ou=people,o=jans",
                    SimpleUser.class, null, SearchProjection.distinct("jansStatus"), 0, 100);
            LOG.info("Distinct statuses: {}", distinctRows.getTotalEntriesCount());
            for (EntryData row : distinctRows.getEntries()) {
                LOG.info("Row: {}", row);
            }

            // Distinct mapped to partial beans: only projected properties populated, DN is null
            List<SimpleUser> distinctUsers = sqlEntryManager.findDistinctEntries("ou=people,o=jans",
                    SimpleUser.class, null, SearchProjection.distinct("uid", "jansStatus"), 0, 10);
            for (SimpleUser user : distinctUsers) {
                LOG.info("userId: {}, dn (must be null): {}, customAttributes: {}", user.getUserId(), user.getDn(),
                        user.getCustomAttributes());
            }
        } finally {
            sqlEntryManager.destroy();
        }
    }

}
