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
import io.jans.orm.cloud.spanner.model.SimpleCustomStringUser;
import io.jans.orm.cloud.spanner.model.UserRole;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.cloud.spanner.persistence.SpannerEntryManagerSample;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SpannerCustomStringAttributesSample {

	private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

	private SpannerCustomStringAttributesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		SpannerEntryManagerSample sqlEntryManagerSample = new SpannerEntryManagerSample();

		// Create SQL entry manager
		SpannerEntryManager sqlEntryManager = sqlEntryManagerSample.createSpannerEntryManager();

		String randomExternalUid = "" + System.currentTimeMillis();
//		String randomExternalUid = "otp:" + System.currentTimeMillis();

		// Add dummy user
		SimpleCustomStringUser newUser = new SimpleCustomStringUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomAttribute("address", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add((new CustomAttribute("jansExtUid", randomExternalUid)).multiValued());

		newUser.setUserRole(UserRole.ADMIN);
		newUser.setMemberOf(Arrays.asList("group_1", "group_2", "group_3"));

		sqlEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());

		// Find added dummy user but use custom class with String values
		SimpleCustomStringUser foundUser = sqlEntryManager.find(SimpleCustomStringUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());
		for (CustomAttribute customAttribute : foundUser.getCustomAttributes()) {
			LOG.info("Found custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
		}

		// Find by jsExternalUid
		Filter jsExternalUidFilter = Filter.createEqualityFilter("jansExtUid", randomExternalUid).multiValued();
		List<SimpleCustomStringUser> foundUsers = sqlEntryManager.findEntries("ou=people,o=jans", SimpleCustomStringUser.class, jsExternalUidFilter);
		for (SimpleCustomStringUser foundUser2 : foundUsers) {
			LOG.info("Found User '{}' by jansExtUid with uid '{}' and key '{}'", foundUser2, foundUser2.getUserId(), foundUser2.getDn());
		}
	}

}
