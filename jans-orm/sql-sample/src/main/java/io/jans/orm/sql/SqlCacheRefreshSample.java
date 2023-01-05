/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimleInumMap;
import io.jans.orm.sql.model.Status;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlCacheRefreshSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlCacheRefreshSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

		String inumbaseDn = "ou=cache-refresh,o=site";

		Filter filterObjectClass = Filter.createEqualityFilter("objectClass",
				"jansInumMap");
		Filter filterStatus = Filter.createNOTFilter(
				Filter.createEqualityFilter("jansStatus", Status.INACTIVE.getValue()));
		Filter filter = Filter.createANDFilter(filterObjectClass, filterStatus);

		List<SimleInumMap> result = sqlEntryManager.findEntries(inumbaseDn, SimleInumMap.class, filter, SearchScope.SUB, null,
				null, 0, 0, 1000);
		System.out.println(result);
    }

}
