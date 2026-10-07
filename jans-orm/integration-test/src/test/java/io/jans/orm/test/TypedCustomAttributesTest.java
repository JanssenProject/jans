/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;
import io.jans.orm.util.StringHelper;

/**
 * Custom attributes with typed values: Date, Boolean and Integer. DB columns should
 * has corresponding types (birthdate: TIMESTAMP, jansActive: BOOLEAN, scimCustomThird: INT).
 * It's automated version of *CustomObjectAttributesSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class TypedCustomAttributesTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private SimpleUser persistedUser;
	private Date birthdate;

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
	public void createUserWithTypedAttributes() {
		String inum = getRandomInum();
		birthdate = new Date();

		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		newUser.setUserId("sample_user_" + inum);
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_value"));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("birthdate", birthdate));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansActive", false));
		// Requires custom attribute in table with INT type
		newUser.getCustomAttributes().add(new CustomObjectAttribute("scimCustomThird", 18));

		entryManager.persist(newUser);

		persistedUser = newUser;
	}

	@Test(dependsOnMethods = "createUserWithTypedAttributes")
	public void readTypedAttributes() {
		SimpleUser foundUser = entryManager.find(SimpleUser.class, persistedUser.getDn());

		assertNotNull(foundUser);

		Object birthdateValue = foundUser.getAttribute("birthdate");
		assertNotNull(birthdateValue);
		assertTrue(birthdateValue instanceof Date, "birthdate should be Date but is " + birthdateValue.getClass());

		Object activeValue = foundUser.getAttribute("jansActive");
		assertNotNull(activeValue);
		assertTrue(activeValue instanceof Boolean, "jansActive should be Boolean but is " + activeValue.getClass());
		assertEquals(activeValue, Boolean.FALSE);

		Object intValue = foundUser.getAttribute("scimCustomThird");
		assertNotNull(intValue);
		assertTrue(intValue instanceof Integer, "scimCustomThird should be Integer but is " + intValue.getClass());
		assertEquals(intValue, Integer.valueOf(18));
	}

	@Test(dependsOnMethods = "readTypedAttributes")
	public void searchByTypedAttributes() {
		Filter uidFilter = Filter.createEqualityFilter("uid", persistedUser.getUserId());

		Filter booleanFilter = Filter.createANDFilter(uidFilter, Filter.createEqualityFilter("jansActive", false));
		assertEquals(entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, booleanFilter).size(), 1);

		Filter integerFilter = Filter.createANDFilter(uidFilter, Filter.createEqualityFilter("scimCustomThird", 18));
		assertEquals(entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, integerFilter).size(), 1);
	}

	@Test(dependsOnMethods = "searchByTypedAttributes")
	public void clearAttributeValueWithMerge() {
		SimpleUser foundUser = entryManager.find(SimpleUser.class, persistedUser.getDn());

		for (Iterator<CustomObjectAttribute> it = foundUser.getCustomAttributes().iterator(); it.hasNext();) {
			CustomObjectAttribute attr = it.next();
			if (StringHelper.equalsIgnoreCase(attr.getName(), "jansGuid")) {
				attr.setValue("");
				break;
			}
		}

		entryManager.merge(foundUser);

		SimpleUser updatedUser = entryManager.find(SimpleUser.class, persistedUser.getDn());
		Object guidValue = updatedUser.getAttribute("jansGuid");
		assertTrue((guidValue == null) || StringHelper.isEmpty(String.valueOf(guidValue)),
				"jansGuid should be cleared but is '" + guidValue + "'");

		// Typed values should stay intact after merge
		assertEquals(updatedUser.getAttribute("jansActive"), Boolean.FALSE);
		assertEquals(updatedUser.getAttribute("scimCustomThird"), Integer.valueOf(18));
	}

}
