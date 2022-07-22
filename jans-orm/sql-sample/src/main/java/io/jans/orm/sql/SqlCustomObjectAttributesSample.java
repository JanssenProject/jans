/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.model.UserRole;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlCustomObjectAttributesSample {

	private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

	private SqlCustomObjectAttributesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

		// Create SQL entry manager
		SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

		// Add dummy user
		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomObjectAttribute("address", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_value"));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("birthdate", new Date()));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansActive", false));
		
		// Require cusom attribute in table with age: INT type
		newUser.getCustomAttributes().add(new CustomObjectAttribute("scimCustomThird", 18));

		newUser.setUserRole(UserRole.ADMIN);
		newUser.setMemberOf(Arrays.asList("group_1", "group_2", "group_3"));

		sqlEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());

		// Find added dummy user
		SimpleUser foundUser = sqlEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());
		for (CustomObjectAttribute customAttribute : foundUser.getCustomAttributes()) {
			if (customAttribute.getValue() instanceof Date) {
				LOG.info("Found date custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValue() instanceof Integer) {
				LOG.info("Found integer custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValue() instanceof Boolean) {
				LOG.info("Found boolean custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValues().size() > 1) {
				LOG.info("Found list custom attribute '{}' with value '{}', multiValued: {}", customAttribute.getName(), customAttribute.getValues(), customAttribute.isMultiValued());
			}
		}
		
		for (Iterator<CustomObjectAttribute> it = foundUser.getCustomAttributes().iterator(); it.hasNext();) {
			CustomObjectAttribute attr = (CustomObjectAttribute) it.next();
			if (StringHelper.equalsIgnoreCase(attr.getName(), "jansGuid")) {
				attr.setValue("");
				break;
			}
		}
		sqlEntryManager.merge(foundUser);

		// Find updated dummy user
		SimpleUser foundUser2 = sqlEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser2, foundUser2.getUserId(), foundUser2.getDn());

		LOG.info("Custom attributes after merge '{}'", foundUser2.getCustomAttributes());
		for (CustomObjectAttribute customAttribute : foundUser2.getCustomAttributes()) {
			if (customAttribute.getValue() instanceof Date) {
				LOG.info("Found date custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValue() instanceof Integer) {
				LOG.info("Found integer custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValue() instanceof Boolean) {
				LOG.info("Found boolean custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
			} else if (customAttribute.getValues().size() > 1) {
				LOG.info("Found list custom attribute '{}' with value '{}', multiValued: {}", customAttribute.getName(), customAttribute.getValues(), customAttribute.isMultiValued());
			}

		}

		// Find added dummy user by numeric attribute
		Filter filter = Filter.createGreaterOrEqualFilter("scimCustomThird", 16);
		List<SimpleUser> foundUsers = sqlEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, filter);
		if (foundUsers.size() > 0) {
			foundUser = foundUsers.get(0);
			LOG.info("Found User '{}' by filter '{}' with uid '{}' and key '{}'", foundUser, filter, foundUser, foundUser);
		} else {
			LOG.error("Can't find User by filter '{}'", filter);
		}
	}

}
