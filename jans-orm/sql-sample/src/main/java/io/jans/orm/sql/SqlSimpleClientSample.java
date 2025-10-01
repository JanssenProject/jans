/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleClient;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;
import io.jans.orm.util.ArrayHelper;

/**
 * @author Yuriy Movchan Date: 05/26/2021
 */
public final class SqlSimpleClientSample {

	private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

	private SqlSimpleClientSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

		// Create SQL entry manager
		SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

		SimpleClient newClient = new SimpleClient();
		newClient.setDn("inum=test_acr2,ou=client,o=jans");
		newClient.setDefaultAcrValues(new String[] {"test_acr2"});
		newClient.setClientName("test_acr2");

		sqlEntryManager.persist(newClient);

		Filter presenceFilter = Filter.createEqualityFilter("displayName", "test_acr2");
		List<SimpleClient> results = sqlEntryManager.findEntries("ou=client,o=jans", SimpleClient.class, presenceFilter);
		for (SimpleClient client : results) {
			String[] acrs = client.getDefaultAcrValues();
			if (ArrayHelper.isNotEmpty(acrs)) {
				System.out.println(Arrays.toString(acrs));
			}
		}

		sqlEntryManager.remove(newClient.getDn(), SimpleClient.class);
	}

}
