/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleUser;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 09/16/2019
 */
public final class CouchbaseCustomMultiValuedTypesSample {

	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

	private CouchbaseCustomMultiValuedTypesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

		// Create Couchbase entry manager
		CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

		// Add dummy user
		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomObjectAttribute("streetAddress", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("test", "test_value"));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("fuzzy", "test_value"));
		newUser.setMemberOf(Arrays.asList("group_1", "group_2", "group_3"));
		newUser.setAttributeValue("givenName", "john");

		couchbaseEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());
		LOG.info("Persisted custom attributes '{}'", newUser.getCustomAttributes());

		// Find added dummy user
		SimpleUser foundUser = couchbaseEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());

		// Update custom attributes
		foundUser.setAttributeValues("streetAddress", Arrays.asList("London", "Texas", "Kiev", "Dublin"));
		foundUser.setAttributeValues("test", Arrays.asList("test_value_1", "test_value_2", "test_value_3", "test_value_4"));
		foundUser.setAttributeValues("fuzzy", Arrays.asList("fuzzy_value_1", "fuzzy_value_2"));
		foundUser.setAttributeValue("simple", "simple");
		
		CustomObjectAttribute multiValuedSingleValue = new CustomObjectAttribute("multivalued", "multivalued_single_valued");
		multiValuedSingleValue.setMultiValued(true);
		foundUser.getCustomAttributes().add(multiValuedSingleValue);
		couchbaseEntryManager.merge(foundUser);
		LOG.info("Updated custom attributes '{}'", foundUser.getCustomAttributes());

		// Find updated dummy user
		SimpleUser foundUpdatedUser = couchbaseEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUpdatedUser, foundUpdatedUser.getUserId(), foundUpdatedUser.getDn());

		LOG.info("Cusom attributes '{}'", foundUpdatedUser.getCustomAttributes());

		Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("givenName"), StringHelper.toLowerCase("john"));
		List<SimpleUser> foundUpdatedUsers = couchbaseEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, filter);
		System.out.println(foundUpdatedUsers);
		
	}

}
