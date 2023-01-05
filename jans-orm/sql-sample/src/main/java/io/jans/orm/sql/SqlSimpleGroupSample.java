/*
 * oxCore is available under the MIT License (2014). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleGroup;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 05/26/2021
 */
public final class SqlSimpleGroupSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlSimpleGroupSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();
        
        String personDN = "inum=ae8c7ff9-def7-4b42-8147-de8380617a37,ou=people,o=jans";
		Filter ownerFilter = Filter.createEqualityFilter("owner", personDN);
		Filter memberFilter = Filter.createEqualityFilter("member", personDN);
		Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

		List<SimpleGroup> result = sqlEntryManager.findEntries("ou=groups,o=jans", SimpleGroup.class, searchFilter, 1);
		System.out.println(result);
    }

}
