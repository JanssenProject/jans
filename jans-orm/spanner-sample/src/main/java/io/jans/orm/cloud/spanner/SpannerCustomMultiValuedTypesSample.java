/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.model.SimpleUser;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.cloud.spanner.persistence.SpannerEntryManagerSample;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SpannerCustomMultiValuedTypesSample {

	private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

	private SpannerCustomMultiValuedTypesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		SpannerEntryManagerSample sqlEntryManagerSample = new SpannerEntryManagerSample();

		// Create SQL entry manager
		SpannerEntryManager sqlEntryManager = sqlEntryManagerSample.createSpannerEntryManager();

		// Add dummy user
		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansOptOuts", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansExtUid", "test_value").multiValued());
		newUser.getCustomAttributes().add(new CustomObjectAttribute("jansPPID", "test_value").multiValued());
		newUser.setMemberOf(Arrays.asList("group_1", "group_2", "group_3"));
		newUser.setAttributeValue("givenName", "john");

		sqlEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());
		LOG.info("Persisted custom attributes '{}'", newUser.getCustomAttributes());

		// Find added dummy user
		SimpleUser foundUser = sqlEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());
		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());

		// Dump custom attributes
		for (CustomObjectAttribute attr : foundUser.getCustomAttributes()) {
			System.out.println(attr.getName() + " - " + attr.getValues());
		}

		// Update custom attributes
		foundUser.setAttributeValues("jansOptOuts", Arrays.asList("London", "Texas", "Kiev", "Dublin"));
		foundUser.setAttributeValues("jansExtUid", Arrays.asList("test_value_11", "test_value_22", "test_value_33", "test_value_44"));
		foundUser.setAttributeValues("jansExtUid", Arrays.asList(11, 22, 33, 44));
		foundUser.setAttributeValues("jansPPID", Arrays.asList("fuzzy_value_1", "fuzzy_value_2"));
		foundUser.setAttributeValue("jansGuid", "simple");
		
		CustomObjectAttribute multiValuedSingleValue = new CustomObjectAttribute("jansAssociatedClnt", "multivalued_single_valued");
		multiValuedSingleValue.setMultiValued(true);
		foundUser.getCustomAttributes().add(multiValuedSingleValue);
		sqlEntryManager.merge(foundUser);
		LOG.info("Updated custom attributes '{}'", foundUser.getCustomAttributes());

		// Find updated dummy user
		SimpleUser foundUpdatedUser = sqlEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUpdatedUser, foundUpdatedUser.getUserId(), foundUpdatedUser.getDn());
		LOG.info("Cusom attributes '{}'", foundUpdatedUser.getCustomAttributes());

		// Dump custom attributes
		for (CustomObjectAttribute attr : foundUser.getCustomAttributes()) {
			System.out.println(attr.getName() + " - " + attr.getValues());
		}

		Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("givenName"), StringHelper.toLowerCase("john"));
		List<SimpleUser> foundUpdatedUsers = sqlEntryManager.findEntries("o=jans", SimpleUser.class, filter);
		System.out.println(foundUpdatedUsers);
	}

}
