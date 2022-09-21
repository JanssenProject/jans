/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleGroup;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlCheckGroupSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlCheckGroupSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

		// EQ -- String
		Filter searchFilter = Filter.create("(&(dn=inum=60B7,ou=groups,o=gluu)(|(owner=inum=78c0ea07-d9db-4ccd-9039-4911a6426a0c,ou=people,o=gluu)(member=inum=78c0ea07-d9db-4ccd-9039-4911a6426a0c,ou=people,o=gluu)))");

		boolean isMemberOrOwner = sqlEntryManager.findEntries("inum=60B7,ou=groups,o=gluu", SimpleGroup.class, searchFilter, 1).size() > 0;
		System.out.println(isMemberOrOwner);
    }

}