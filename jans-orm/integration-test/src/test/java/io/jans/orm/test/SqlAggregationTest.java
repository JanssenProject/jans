/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;

/**
 * SQL specific aggregated searches: GROUP BY with count and DISTINCT projections.
 * It's automated version of SqlGroupBySample and SqlDistinctSample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class SqlAggregationTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private String markerGuid;
	private List<String> createdDns = new ArrayList<>();

	@BeforeClass
	public void checkPersistenceType() {
		// Parent class @BeforeClass runs first, so entryManager is already initialized
		requirePersistenceType(SQL);
	}

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		for (String dn : createdDns) {
			try {
				entryManager.remove(dn, SimpleUser.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createUsersForAggregation() {
		markerGuid = "aggregation_" + getRandomInum();

		createUser("active", markerGuid);
		createUser("active", markerGuid);
		createUser("active", markerGuid);
		createUser("inactive", markerGuid);
		createUser("inactive", markerGuid);
	}

	@Test(dependsOnMethods = "createUsersForAggregation")
	public void groupByWithCount() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid);

		SearchProjection projection = SearchProjection.groupBy("jansStatus").count().orderBy("total", SortOrder.DESCENDING);
		PagedResult<EntryData> result = entryManager.findAggregatedEntries(PEOPLE_BASE_DN, SimpleUser.class, filter, projection, 0, 100);

		assertNotNull(result);
		assertEquals(result.getEntries().size(), 2);

		long groupsSum = 0;
		for (EntryData row : result.getEntries()) {
			Object total = row.getAttributeData("total").getValue();
			groupsSum += Long.parseLong(String.valueOf(total));
		}

		// Cross-check: sum of group counts must match countEntries with the same filter
		int allUsers = entryManager.countEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);
		assertEquals(groupsSum, allUsers);
		assertEquals(groupsSum, 5);
	}

	@Test(dependsOnMethods = "createUsersForAggregation")
	public void distinctRows() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid);

		PagedResult<EntryData> distinctRows = entryManager.findAggregatedEntries(PEOPLE_BASE_DN, SimpleUser.class, filter,
				SearchProjection.distinct("jansStatus"), 0, 100);

		assertNotNull(distinctRows);
		assertEquals(distinctRows.getEntries().size(), 2);
	}

	@Test(dependsOnMethods = "createUsersForAggregation")
	public void distinctEntries() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid);

		List<SimpleUser> distinctUsers = entryManager.findDistinctEntries(PEOPLE_BASE_DN, SimpleUser.class, filter,
				SearchProjection.distinct("uid", "jansStatus"), 0, 10);

		assertNotNull(distinctUsers);
		assertEquals(distinctUsers.size(), 5);
		assertTrue(distinctUsers.get(0).getUserId().startsWith("aggregation_user_"));
	}

	private void createUser(String status, String guid) {
		String inum = getRandomInum();

		SimpleUser user = new SimpleUser();
		user.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		user.setUserId("aggregation_user_" + inum);
		user.getCustomAttributes().add(new CustomObjectAttribute("jansStatus", status));
		user.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", guid));

		entryManager.persist(user);
		createdDns.add(user.getDn());
	}

}
