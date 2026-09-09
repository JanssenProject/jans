/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleClient;
import io.jans.orm.test.model.SimpleUser;

/**
 * SQL injection attempts on entry insertion and search. Values with SQL should be
 * stored/compared as plain data because ORM binds them as prepared statement
 * parameters. It's automated version of SqlInjectionSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class SqlInjectionTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private SimpleUser persistedUser;

	@BeforeClass
	public void checkPersistenceType() {
		// Parent class @BeforeClass runs first, so entryManager is already initialized
		requirePersistenceType(SQL);
	}

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (persistedUser != null)) {
			try {
				entryManager.remove(persistedUser.getDn(), SimpleUser.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void injectionOnInsert() {
		checkInjectionValueRoundTrip("105 OR 1=1");
		checkInjectionValueRoundTrip("DROP TABLE jansClnt");
	}

	@Test(dependsOnMethods = "injectionOnInsert")
	public void injectionOnSearch() {
		Filter filter1 = Filter.createEqualityFilter("jansStatus", "105 OR 1=1");
		List<SimpleUser> users1 = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter1, SearchScope.SUB, null, null, 10, 0, 0);
		assertEquals(users1.size(), 0);

		Filter filter2 = Filter.createEqualityFilter("jansStatus", "DROP TABLE jansClnt");
		List<SimpleUser> users2 = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter2, SearchScope.SUB, null, null, 10, 0, 0);
		assertEquals(users2.size(), 0);

		Filter filter3 = Filter.createORFilter(Filter.createEqualityFilter("uid", "\" or \"\"=\""),
				Filter.createEqualityFilter("userPassword", "105 OR 1=1"));
		List<SimpleUser> users3 = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter3, SearchScope.SUB, null, null, 10, 0, 0);
		assertEquals(users3.size(), 0);
	}

	@Test(dependsOnMethods = "injectionOnSearch")
	public void clientsTableStillExists() {
		// Check that "DROP TABLE jansClnt" values were processed as plain data
		List<SimpleClient> clients = entryManager.findEntries("ou=clients,o=jans", SimpleClient.class, null, 1);

		assertNotNull(clients);
	}

	private void checkInjectionValueRoundTrip(String value) {
		String inum = getRandomInum();

		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		newUser.setUserId(value);

		entryManager.persist(newUser);
		persistedUser = newUser;

		SimpleUser foundUser = entryManager.find(SimpleUser.class, newUser.getDn());
		assertNotNull(foundUser);
		assertEquals(foundUser.getUserId(), value);

		entryManager.remove(newUser.getDn(), SimpleUser.class);
		assertFalse(entryManager.contains(newUser.getDn(), SimpleUser.class));
		persistedUser = null;
	}

}
