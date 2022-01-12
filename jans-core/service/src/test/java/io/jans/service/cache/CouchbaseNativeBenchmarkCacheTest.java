/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Movchan Date: 11/29/2019
 */
public class CouchbaseNativeBenchmarkCacheTest {

	private CouchbaseEntryManager entryManager;

	@BeforeClass
	public void init() {
		this.entryManager = createCouchbaseEntryManager();
	}

	@AfterClass
	public void destroy() {
		this.entryManager.destroy();
	}

	@Test(enabled = false, threadPoolSize = 300, invocationCount = 10000) // manual
	public void couchbaseCacheProvider() {
		final String baseDn = "ou=cache,o=jans";

		final CacheConfiguration cacheConfiguration = new CacheConfiguration();
		cacheConfiguration.setNativePersistenceConfiguration(new NativePersistenceConfiguration());
		cacheConfiguration.getNativePersistenceConfiguration().setBaseDn("");

		NativePersistenceCacheProvider provider = new NativePersistenceCacheProvider();
		provider.configure(cacheConfiguration, entryManager);

		provider.setBaseDn(baseDn);

		Map<String, String> sessionAttributes = new HashMap<>();
		sessionAttributes.put("attr1", "value1");
		sessionAttributes.put("attr2", "value2");

		SampleSessionId sessionId = new SampleSessionId();
		sessionId.setId(UUID.randomUUID().toString());
		sessionId.setDn(sessionId.getId());
		sessionId.setAuthenticationTime(new Date());
		sessionId.setState(SessionIdState.AUTHENTICATED);
		sessionId.setSessionAttributes(sessionAttributes);

		provider.put(130, sessionId.getId(), sessionId);

		final SampleSessionId fromCache = (SampleSessionId) provider.get(sessionId.getId());

		assertNotNull(fromCache, "Failed to get by key: " + sessionId.getId() + " Cache key: " + provider.hashKey(sessionId.getId()));
		assertEquals(fromCache.getId(), sessionId.getId(), "Get session with invaid key: " + sessionId.getId());
	}

	// MODIFY ACCORDING TO YOUR SERVER
	private Properties getSampleConnectionProperties() {
		Properties connectionProperties = new Properties();

		connectionProperties.put("couchbase.servers", "test.gluu.org");
		connectionProperties.put("couchbase.auth.userName", "admin");
		connectionProperties.put("couchbase.auth.userPassword", "test");
		connectionProperties.put("couchbase.buckets", "gluu, gluu_cache");

		connectionProperties.put("couchbase.bucket.default", "gluu");
		connectionProperties.put("couchbase.bucket.gluu_cache.mapping", "cache");

		connectionProperties.put("couchbase.password.encryption.method", "CRYPT-SHA-256");

		return connectionProperties;
	}

	private CouchbaseEntryManager createCouchbaseEntryManager() {
		CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
		couchbaseEntryManagerFactory.create();
		Properties connectionProperties = getSampleConnectionProperties();

		CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);
		System.out.println("Created CouchbaseEntryManager: " + couchbaseEntryManager);

		return couchbaseEntryManager;
	}

}
