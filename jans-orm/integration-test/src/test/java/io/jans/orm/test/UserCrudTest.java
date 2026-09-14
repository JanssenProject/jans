/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;

/**
 * User entry CRUD with custom attributes and authentication. It's automated
 * version of SqlSample/CouchbaseSample/LdapSample and *AuthenticationSample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class UserCrudTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private static final String USER_PASSWORD = "user_pwd_" + System.currentTimeMillis();

	private SimpleUser persistedUser;

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
	public void createUser() {
		String inum = getRandomInum();

		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		newUser.setUserId("sample_user_" + inum);
		newUser.setUserPassword(USER_PASSWORD);
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansOptOuts", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_guid_" + inum));

		entryManager.persist(newUser);

		persistedUser = newUser;
	}

	@Test(dependsOnMethods = "createUser")
	public void findUserByDn() {
		SimpleUser foundUser = entryManager.find(SimpleUser.class, persistedUser.getDn());

		assertNotNull(foundUser);
		assertEquals(foundUser.getUserId(), persistedUser.getUserId());
		assertNotNull(foundUser.getAttribute("jansGuid"));
	}

	@Test(dependsOnMethods = "createUser")
	public void findUserByFilter() {
		Filter filter = Filter.createEqualityFilter("uid", persistedUser.getUserId());
		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);

		assertNotNull(users);
		assertEquals(users.size(), 1);
		assertEquals(users.get(0).getUserId(), persistedUser.getUserId());
	}

	@Test(dependsOnMethods = "createUser")
	public void findUserByLowercaseFilter() {
		Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"),
				persistedUser.getUserId().toLowerCase());
		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);

		assertNotNull(users);
		assertEquals(users.size(), 1);
	}

	@Test(dependsOnMethods = "createUser")
	public void containsUser() {
		assertTrue(entryManager.contains(persistedUser.getDn(), SimpleUser.class));
	}

	@Test(dependsOnMethods = "createUser")
	public void authenticateUser() {
		boolean resultByDn = entryManager.authenticate(persistedUser.getDn(), SimpleUser.class, USER_PASSWORD);
		assertTrue(resultByDn);

		boolean resultByUid = entryManager.authenticate(PEOPLE_BASE_DN, SimpleUser.class, persistedUser.getUserId(), USER_PASSWORD);
		assertTrue(resultByUid);

		boolean resultWrongPassword = entryManager.authenticate(persistedUser.getDn(), SimpleUser.class, "wrong_password");
		assertFalse(resultWrongPassword);
	}

	@Test(dependsOnMethods = { "findUserByDn", "findUserByFilter", "findUserByLowercaseFilter", "containsUser", "authenticateUser" })
	public void updateUser() {
		SimpleUser user = entryManager.find(SimpleUser.class, persistedUser.getDn());

		user.setAttributeValue("jansGuid", "updated_guid");
		user.setAttributeValues("jansOptOuts", Arrays.asList("London", "Texas", "Kiev", "Dublin"));

		entryManager.merge(user);

		SimpleUser updatedUser = entryManager.find(SimpleUser.class, persistedUser.getDn());
		assertEquals(String.valueOf(updatedUser.getAttribute("jansGuid")), "updated_guid");
		assertEquals(updatedUser.getAttributeValues("jansOptOuts").size(), 4);
	}

	@Test(dependsOnMethods = "updateUser")
	public void countUsers() {
		Filter filter = Filter.createEqualityFilter("uid", persistedUser.getUserId());
		int count = entryManager.countEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);

		assertEquals(count, 1);
	}

	@Test(dependsOnMethods = "countUsers")
	public void deleteUser() {
		entryManager.remove(persistedUser.getDn(), SimpleUser.class);

		assertFalse(entryManager.contains(persistedUser.getDn(), SimpleUser.class));

		persistedUser = null;
	}

}
