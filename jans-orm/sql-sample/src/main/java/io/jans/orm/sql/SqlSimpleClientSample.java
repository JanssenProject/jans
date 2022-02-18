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
public class SqlSimpleClientSample {

	private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

	private SqlSimpleClientSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

		// Create SQL entry manager
		SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

		SimpleClient newClient = new SimpleClient();
		newClient.setDn("inum=test_clnt2,ou=client,o=gluu");
		newClient.setDefaultAcrValues(new String[] { "test_clnt2_acr" });
		newClient.setClientName("test_clnt2");

		sqlEntryManager.persist(newClient);

		Filter presenceFilter = Filter.createEqualityFilter("displayName", "test_clnt2");
		List<SimpleClient> results = sqlEntryManager.findEntries("ou=test_clnt2,o=gluu", SimpleClient.class,
				presenceFilter);
		for (SimpleClient client : results) {
			String[] acrs = client.getDefaultAcrValues();
			if (ArrayHelper.isNotEmpty(acrs)) {
				System.out.println(Arrays.toString(acrs));
			}
		}

		sqlEntryManager.remove(newClient.getDn(), SimpleClient.class);
	}

}
