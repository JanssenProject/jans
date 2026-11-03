/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;

/**
 * Sorted search with limit which doesn't calculate total entries count.
 *
 * @author Yuriy Movchan Date: 10/25/2026
 */
public class SqlSortedFindTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private String markerGuid;
	private List<String> createdDns = new ArrayList<>();

	@BeforeClass
	public void checkPersistenceType() {
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
	public void createUsers() {
		markerGuid = "sorted_" + getRandomInum();

		for (String suffix : new String[] { "c", "a", "e", "b", "d" }) {
			String inum = getRandomInum();
			SimpleUser user = new SimpleUser();
			user.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
			user.setUserId(markerGuid + "_" + suffix);
			user.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", markerGuid));

			entryManager.persist(user);
			createdDns.add(user.getDn());
		}
	}

	@Test(dependsOnMethods = "createUsers")
	public void findLastByDescendingSort() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid);

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter, new String[] { "uid" }, "uid",
				SortOrder.DESCENDING, 0, 1, 1);

		assertEquals(users.size(), 1);
		assertEquals(users.get(0).getUserId(), markerGuid + "_e");
	}

	@Test(dependsOnMethods = "createUsers")
	public void findPageByAscendingSort() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid);

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter, new String[] { "uid" }, "uid",
				SortOrder.ASCENDING, 1, 2, 2);

		assertEquals(users.size(), 2);
		assertEquals(users.get(0).getUserId(), markerGuid + "_b");
		assertEquals(users.get(1).getUserId(), markerGuid + "_c");
	}

	@Test(dependsOnMethods = "createUsers")
	public void findNoMatches() {
		Filter filter = Filter.createEqualityFilter("jansGuid", markerGuid + "_missing");

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter, new String[] { "uid" }, "uid",
				SortOrder.DESCENDING, 0, 1, 1);

		assertTrue(users.isEmpty());
	}

}
