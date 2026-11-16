/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.jans.lock.service.trace.ingest.TraceBatchIngestDifferentialTest;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.orm.PersistenceEntryManager;

/**
 * The batch-versus-one-by-one scenarios of {@link TraceBatchIngestDifferentialTest} over
 * {@link OrmTraceStore} and a live SQL database (task 08, deliverable 4). Opt-in through the
 * database profile property {@value DatabaseProfile#PROPERTY}; skipped without it. The two
 * harnesses share the database, so each writes its own throw-away domain, removed afterwards.
 *
 * <pre>
 * cd jans-lock/lock-server
 * mvn -o -pl model,service -am -Ddownload.plugin.skip=true test \
 *     -Dtest=OrmDatabaseBatchIngestDifferentialTest -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dlock.benchmark.profile=/path/to/jans-orm/integration-test/profiles/jans-dev.jans.info/conf
 * </pre>
 */
@EnabledIfSystemProperty(named = DatabaseProfile.PROPERTY, matches = ".+")
class OrmDatabaseBatchIngestDifferentialTest extends TraceBatchIngestDifferentialTest {

	private static PersistenceEntryManager entryManager;

	private final List<String> domains = new ArrayList<>();

	@BeforeAll
	static void connect() throws Exception {
		entryManager = DatabaseProfile.connect();
	}

	@AfterAll
	static void disconnect() {
		if (entryManager != null) {
			entryManager.destroy();
		}
	}

	@AfterEach
	void removeDomains() {
		for (String domain : domains) {
			DatabaseProfile.removeDomain(entryManager, domain);
		}
		domains.clear();
	}

	@Override
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness(TraceTestCrypto.javaProvider(), keys -> OrmTraceStores.create(entryManager, keys));
	}

	@Override
	protected String sequentialDomain() {
		return remember(DatabaseProfile.newDomain("diff-seq"));
	}

	@Override
	protected String batchDomain() {
		return remember(DatabaseProfile.newDomain("diff-bat"));
	}

	private String remember(String domain) {
		domains.add(domain);
		return domain;
	}

}
