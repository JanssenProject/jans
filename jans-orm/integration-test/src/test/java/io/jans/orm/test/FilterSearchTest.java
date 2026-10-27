/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;

/**
 * Complex and SCIM like searches: OR over substring filters, substring search in
 * multi-valued attributes, greater-or-equal by timestamp and NOT filter. It's
 * automated version of SqlComplexFilterSample, SqlScimUserSearchSample,
 * SqlScimSubstringSearchSample and filter part of SqlLinkSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class FilterSearchTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private String marker;
	private List<String> createdDns = new ArrayList<>();

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
	public void createUsersForSearch() {
		marker = "filter_" + getRandomInum();

		// Active user with mail, givenName and multi-valued mobile
		SimpleUser user1 = buildUser("active");
		user1.setAttributeValue("mail", marker + "@gluu.org");
		user1.setAttributeValue("givenName", "givenname_" + marker);
		user1.getCustomAttributes().add(new CustomObjectAttribute("mobile", Arrays.asList("+1-234-" + marker, "+1-567-" + marker)).multiValued());
		entryManager.persist(user1);

		// Inactive user with sn and nickname
		SimpleUser user2 = buildUser("inactive");
		user2.setAttributeValue("sn", "sn_" + marker);
		user2.setAttributeValue("nickname", "nickname_" + marker);
		entryManager.persist(user2);
	}

	@Test(dependsOnMethods = "createUsersForSearch")
	public void searchByOrOverSubstringFilters() {
		String[] targetArray = new String[] { marker };

		Filter mailFilter = Filter.createSubstringFilter("mail", null, targetArray, null);
		Filter givenNameFilter = Filter.createSubstringFilter("givenName", null, targetArray, null);
		Filter nicknameFilter = Filter.createSubstringFilter("nickname", null, targetArray, null);
		Filter snFilter = Filter.createSubstringFilter("sn", null, targetArray, null);
		Filter orFilter = Filter.createORFilter(mailFilter, givenNameFilter, nicknameFilter, snFilter);

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, orFilter);

		assertNotNull(users);
		assertEquals(users.size(), 2);
	}

	@Test(dependsOnMethods = "createUsersForSearch")
	public void searchBySubstringInMultiValuedAttribute() {
		Filter mobileFilter = Filter.createSubstringFilter("mobile", null, new String[] { "-234-" + marker }, null).multiValued();

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, mobileFilter);

		assertNotNull(users);
		assertEquals(users.size(), 1);
	}

	@Test(dependsOnMethods = "createUsersForSearch")
	public void searchBySubstringEnd() {
		// SCIM like search: mail ends with domain OR phone value contains pattern
		Filter filter = Filter.createORFilter(
				Filter.createSubstringFilter("mobile", null, new String[] { "+1-567-" + marker }, null).multiValued(),
				Filter.createANDFilter(Filter.createSubstringFilter("mail", null, new String[] { marker }, null),
						Filter.createSubstringFilter("mail", null, null, "gluu.org")));

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);

		assertNotNull(users);
		assertEquals(users.size(), 1);
	}

	@Test(dependsOnMethods = "createUsersForSearch")
	public void searchByGreaterOrEqualTimestamp() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -1);
		Filter markerFilter = Filter.createSubstringFilter("uid", null, new String[] { marker }, null);

		Filter geFilter = Filter.createGreaterOrEqualFilter("jansCreationTimestamp", TIMESTAMP_FORMAT.format(calendar.getTime()));
		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, Filter.createANDFilter(markerFilter, geFilter));

		assertNotNull(users);
		assertEquals(users.size(), 2);

		// No users created after future timestamp
		calendar.add(Calendar.HOUR, 2);
		Filter geFutureFilter = Filter.createGreaterOrEqualFilter("jansCreationTimestamp", TIMESTAMP_FORMAT.format(calendar.getTime()));
		List<SimpleUser> futureUsers = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, Filter.createANDFilter(markerFilter, geFutureFilter));

		assertNotNull(futureUsers);
		assertEquals(futureUsers.size(), 0);
	}

	@Test(dependsOnMethods = "createUsersForSearch")
	public void searchWithNotFilter() {
		Filter markerFilter = Filter.createEqualityFilter("jansGuid", marker);
		Filter notInactiveFilter = Filter.createNOTFilter(Filter.createEqualityFilter("jansStatus", "inactive"));

		List<SimpleUser> users = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class,
				Filter.createANDFilter(markerFilter, notInactiveFilter));

		assertNotNull(users);
		assertEquals(users.size(), 1);
	}

	private SimpleUser buildUser(String status) {
		String inum = getRandomInum();

		SimpleUser user = new SimpleUser();
		user.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		user.setUserId("user_" + marker + "_" + inum);
		user.getCustomAttributes().add(new CustomObjectAttribute("jansStatus", status));
		user.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", marker));
		user.getCustomAttributes().add(new CustomObjectAttribute("jansCreationTimestamp", new Date()));

		createdDns.add(user.getDn());

		return user;
	}

}
