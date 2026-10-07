/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleCustomStringUser;
import io.jans.orm.test.model.SimpleUser;
import io.jans.orm.util.StringHelper;

/**
 * Custom attributes: multi-valued, typed values and read back with String values entry.
 * It's automated version of *CustomMultiValuedTypesSample, *CustomObjectAttributesSample
 * and *CustomStringAttributesSample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class CustomAttributesTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private SimpleUser persistedUser;
	private String givenName;

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
	public void createUserWithCustomAttributes() {
		String inum = getRandomInum();
		givenName = "john_" + inum;

		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		newUser.setUserId("sample_user_" + inum);
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansOptOuts", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansExtUid", "test_value").multiValued());
		newUser.setMemberOf(Arrays.asList("group_1", "group_2", "group_3"));
		newUser.setAttributeValue("givenName", givenName);

		entryManager.persist(newUser);

		persistedUser = newUser;
	}

	@Test(dependsOnMethods = "createUserWithCustomAttributes")
	public void readCustomAttributes() {
		SimpleUser foundUser = entryManager.find(SimpleUser.class, persistedUser.getDn());

		assertNotNull(foundUser);
		assertEquals(foundUser.getAttributeValues("jansOptOuts").size(), 3);
		assertEquals(foundUser.getAttributeValues("jansExtUid").size(), 1);
		assertEquals(foundUser.getMemberOf().size(), 3);
		assertEquals(String.valueOf(foundUser.getAttribute("givenName")), givenName);
	}

	@Test(dependsOnMethods = "readCustomAttributes")
	public void updateCustomAttributes() {
		SimpleUser foundUser = entryManager.find(SimpleUser.class, persistedUser.getDn());

		foundUser.setAttributeValues("jansOptOuts", Arrays.asList("London", "Texas", "Kiev", "Dublin"));
		foundUser.setAttributeValues("jansExtUid", Arrays.asList("test_value_11", "test_value_22", "test_value_33", "test_value_44"));

		entryManager.merge(foundUser);

		SimpleUser updatedUser = entryManager.find(SimpleUser.class, persistedUser.getDn());
		assertEquals(updatedUser.getAttributeValues("jansOptOuts").size(), 4);
		assertEquals(updatedUser.getAttributeValues("jansExtUid").size(), 4);
	}

	@Test(dependsOnMethods = "updateCustomAttributes")
	public void searchByLowercaseCustomAttribute() {
		Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("givenName"), StringHelper.toLowerCase(givenName));
		List<SimpleUser> foundUsers = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);

		assertNotNull(foundUsers);
		assertEquals(foundUsers.size(), 1);
	}

	@Test(dependsOnMethods = "updateCustomAttributes")
	public void readCustomAttributesAsStrings() {
		SimpleCustomStringUser foundUser = entryManager.find(SimpleCustomStringUser.class, persistedUser.getDn());

		assertNotNull(foundUser);
		assertEquals(foundUser.getUserId(), persistedUser.getUserId());
	}

}
