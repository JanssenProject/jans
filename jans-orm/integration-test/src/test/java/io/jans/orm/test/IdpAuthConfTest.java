/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import io.jans.orm.test.model.JansConfiguration;

/**
 * Load Jans configuration entry from real DB installation and parse jansDbAuth
 * JSON attribute. It's automated version of SqlIdpAuthConfSample/SpannerIdpAuthConfSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class IdpAuthConfTest extends BaseOrmTest {

	private static final String CONFIGURATION_DN = "ou=configuration,o=jans";

	@Test
	public void loadJansConfiguration() {
		JansConfiguration jansConfiguration = entryManager.find(JansConfiguration.class, CONFIGURATION_DN);

		assertNotNull(jansConfiguration);
		assertNotNull(jansConfiguration.getDn());
	}

}
