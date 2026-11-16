/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

/**
 * The opt-in live database of {@link TraceIngestBenchmark} and of the database-profile tests: the
 * system property {@value #PROPERTY} names a folder with {@code jans-sql.properties} and {@code salt}
 * (the layout of {@code jans-orm/integration-test/profiles/<name>/conf}). Tests gate on it with
 * {@code @EnabledIfSystemProperty(named = DatabaseProfile.PROPERTY, matches = ".+")}.
 *
 * <p>Every test writes into throw-away domains named {@code bench-...} and removes them with
 * {@link #removeDomain}.
 *
 * @author Yuriy Movchan
 */
final class DatabaseProfile {

	static final String PROPERTY = "lock.benchmark.profile";

	private DatabaseProfile() {
	}

	/** @return an entry manager over the configured database; the caller destroys it */
	static PersistenceEntryManager connect() throws IOException {
		return createEntryManager(Paths.get(System.getProperty(PROPERTY)));
	}

	/** @return a fresh throw-away domain id, {@code bench-<prefix>-<random>} */
	static String newDomain(String prefix) {
		return "bench-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/** Deletes every record, receipt, chain and producer key of {@code domain}. */
	static void removeDomain(PersistenceEntryManager entryManager, String domain) {
		Filter filter = Filter.createEqualityFilter("jansTraceDomainId", domain);
		String base = OrmTraceStores.BASE_DN;
		entryManager.remove("ou=records," + base, TraceRecordEntry.class, filter, 1_000_000);
		entryManager.remove("ou=receipts," + base, TraceReceiptEntry.class, filter, 1_000_000);
		entryManager.remove("ou=chains," + base, TraceChainEntry.class, filter, 1_000_000);
		entryManager.remove("ou=keys," + base, TraceProducerKeyEntry.class, filter, 1_000_000);
	}

	/** @return the number of receipt rows of {@code domain} in {@code state} */
	static int countReceipts(PersistenceEntryManager entryManager, String domain, String state) {
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansTraceDomainId", domain),
				Filter.createEqualityFilter("jansTraceReceiptState", state));
		return entryManager.countEntries("ou=receipts," + OrmTraceStores.BASE_DN, TraceReceiptEntry.class, filter);
	}

	/** @return the number of receipt rows of {@code domain} in any state */
	static int countReceipts(PersistenceEntryManager entryManager, String domain) {
		Filter filter = Filter.createEqualityFilter("jansTraceDomainId", domain);
		return entryManager.countEntries("ou=receipts," + OrmTraceStores.BASE_DN, TraceReceiptEntry.class, filter);
	}

	static PersistenceEntryManager createEntryManager(Path confDir) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(confDir.resolve("jans-sql.properties").toFile())) {
			properties.load(in);
		}
		Properties salt = new Properties();
		try (InputStream in = new FileInputStream(confDir.resolve("salt").toFile())) {
			salt.load(in);
		}

		String sslFactoryArg = properties.getProperty("connection.driver-property.sslfactoryarg");
		if ((sslFactoryArg != null) && sslFactoryArg.startsWith("file:") && !new File(sslFactoryArg.substring(5)).isAbsolute()) {
			properties.setProperty("connection.driver-property.sslfactoryarg",
					"file:" + confDir.resolve(sslFactoryArg.substring(5)).toAbsolutePath());
		}

		try {
			Properties decrypted = PropertiesDecrypter.decryptAllProperties(
					StringEncrypter.instance(salt.getProperty("encodeSalt").trim()), properties);
			// The factory expects keys the way the persistence configuration loader produces them: "<type>#<key>"
			Properties prefixed = new Properties();
			for (String name : decrypted.stringPropertyNames()) {
				prefixed.setProperty(SqlEntryManagerFactory.PERSISTENCE_TYPE + "#" + name, decrypted.getProperty(name));
			}
			return new SqlEntryManagerFactory().createEntryManager(prefixed);
		} catch (StringEncrypter.EncryptionException ex) {
			throw new IllegalStateException("Failed to decrypt database properties", ex);
		}
	}

}
