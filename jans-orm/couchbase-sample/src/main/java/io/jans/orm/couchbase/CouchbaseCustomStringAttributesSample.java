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
import io.jans.orm.couchbase.model.SimpleCustomStringUser;
import io.jans.orm.couchbase.model.UserRole;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 09/27/2019
 */
public final class CouchbaseCustomStringAttributesSample {

	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

	private CouchbaseCustomStringAttributesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

		// Create Couchbase entry manager
		CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

		String randomExternalUid = "otp:" + System.currentTimeMillis();

		// Add dummy user
		SimpleCustomStringUser newUser = new SimpleCustomStringUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomAttribute("streetAddress", Arrays.asList("London", "Texas", "Kiev")).setMultiValued(false));
		newUser.getCustomAttributes().add((new CustomAttribute("jansExternalUid", randomExternalUid)).setMultiValued(true));

		newUser.setUserRole(UserRole.ADMIN);
		newUser.setNotes(Arrays.asList("note 1", "note 2", "note 3"));

		couchbaseEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());

		// Find added dummy user but use custom class with String values
		SimpleCustomStringUser foundUser = couchbaseEntryManager.find(SimpleCustomStringUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());
		for (CustomAttribute customAttribute : foundUser.getCustomAttributes()) {
			LOG.info("Found custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValues());
		}

		// Persist as multivalued
		foundUser.setAttributeValues("streetAddress", Arrays.asList("London", "Texas", "Kiev"));
		couchbaseEntryManager.merge(foundUser);

		LOG.info("Updated User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		// Find added dummy user but use custom class with String values
		SimpleCustomStringUser foundUser2 = couchbaseEntryManager.find(SimpleCustomStringUser.class, foundUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser2.getCustomAttributes());
		for (CustomAttribute customAttribute : foundUser2.getCustomAttributes()) {
			LOG.info("Found custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValues());
		}

		// Find by jsExternalUid
		Filter jsExternalUidFilter = Filter.createEqualityFilter("jansExternalUid", randomExternalUid).multiValued();
		List<SimpleCustomStringUser> foundUsers = couchbaseEntryManager.findEntries("ou=people,o=jans", SimpleCustomStringUser.class, jsExternalUidFilter);
		for (SimpleCustomStringUser foundUser3 : foundUsers) {
			LOG.info("Found User '{}' by jsExternalUid with uid '{}' and key '{}'", foundUser3, foundUser3.getUserId(), foundUser3.getDn());
		}
	}

}
