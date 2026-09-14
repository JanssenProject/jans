/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.persistence;

import java.io.File;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.StandalonePersistanceFactoryService;
import io.jans.orm.util.StringHelper;
import io.jans.orm.util.properties.FileConfiguration;

/**
 * Creates shared PersistenceEntryManager from profile configuration the same way
 * as real Jans applications do: jans.base system property points to the profile
 * folder with conf/jans.properties, conf/jans-<type>.properties and conf/salt.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public final class PersistenceTestFixture {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceTestFixture.class);

	private static final String SALT_FILE_NAME = "salt";

	private static boolean initialized = false;
	private static String skipReason = null;

	private static PersistenceEntryManager entryManager;
	private static String basePersistenceType;

	private PersistenceTestFixture() {
	}

	public static synchronized PersistenceEntryManager getEntryManager() {
		init();

		if (entryManager == null) {
			throw new SkipException(skipReason);
		}

		return entryManager;
	}

	public static synchronized String getBasePersistenceType() {
		init();

		if (entryManager == null) {
			throw new SkipException(skipReason);
		}

		return basePersistenceType;
	}

	private static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		String baseDir = System.getProperty("jans.base");
		if (StringHelper.isEmpty(baseDir)) {
			skipReason = "Profile is not specified. Run tests with -Dcfg=<profile name> and jans.base system property";
			LOG.warn(skipReason);
			return;
		}

		String confDir = baseDir + File.separator + "conf" + File.separator;
		File jansProperties = new File(confDir + "jans.properties");
		if (!jansProperties.exists()) {
			skipReason = String.format("Profile configuration '%s' is not found. DB tests are skipped. See profiles/default/README.md", jansProperties.getAbsolutePath());
			LOG.warn(skipReason);
			return;
		}

		try {
			createEntryManager(confDir);
		} catch (Exception ex) {
			LOG.error("Failed to create PersistenceEntryManager from profile", ex);
			throw new IllegalStateException("Failed to create PersistenceEntryManager from profile", ex);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(PersistenceTestFixture::destroy));
	}

	private static void createEntryManager(String confDir) {
		// Load persistence configuration conf/jans.properties + conf/jans-<type>.properties
		StandalonePersistanceFactoryService persistanceFactoryService = new StandalonePersistanceFactoryService();
		PersistenceConfiguration persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration();
		if ((persistenceConfiguration == null) || (persistenceConfiguration.getConfiguration() == null)
				|| !persistenceConfiguration.getConfiguration().isLoaded()) {
			throw new IllegalStateException("Failed to load persistence configuration from profile");
		}

		FileConfiguration baseConfiguration = new FileConfiguration(confDir + "jans.properties");
		String persistenceType = baseConfiguration.getString("persistence.type");
		basePersistenceType = StringHelper.isEmpty(persistenceType) ? null : persistenceType.split("\\.")[0];

		// Load salt and decrypt passwords in connection properties
		FileConfiguration saltConfiguration = new FileConfiguration(confDir + SALT_FILE_NAME);
		if (!saltConfiguration.isLoaded()) {
			throw new IllegalStateException(String.format("Failed to load salt file '%s%s' from profile", confDir, SALT_FILE_NAME));
		}
		String encodeSalt = saltConfiguration.getString("encodeSalt");

		Properties connectionProperties = persistenceConfiguration.getConfiguration().getProperties();
		Properties decryptedConnectionProperties = TestPropertiesDecrypter.decryptAllProperties(connectionProperties, encodeSalt);

		// Create entry manager
		PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService
				.getPersistenceEntryManagerFactory(persistenceConfiguration);
		entryManager = persistenceEntryManagerFactory.createEntryManager(decryptedConnectionProperties);

		LOG.info("Created PersistenceEntryManager '{}' with persistence type '{}'", entryManager, basePersistenceType);
	}

	private static synchronized void destroy() {
		if (entryManager != null) {
			entryManager.destroy();
			entryManager = null;
		}
	}

}
