/*
 * oxCore is available under the MIT License (2014). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;

import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleGroup;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlPagedUserSearchSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlPagedUserSearchSample() {
    }

    public static void main(String[] args) throws InterruptedException {
        // Prepare sample connection details
    	final SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();
        final SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        try {
        	int start = 0;
        	int lastCount = 0;
        	boolean first = true;
        	while (first || (lastCount > 0) ) {
	            PagedResult<SimpleUser> listViewResponse = sqlEntryManager.findPagedEntries("o=gluu", SimpleUser.class, null,
	                    new String[] { "uid", "displayName", "gluuStatus" }, "uid", SortOrder.ASCENDING, start + 1000, 1000, 333);
	            lastCount = listViewResponse.getEntriesCount();
	            start += 1000;
	            first = false;
	            System.out.println("Loaded count: " + lastCount + " / " + listViewResponse.getTotalEntriesCount());

	            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
        	}
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

    }

}