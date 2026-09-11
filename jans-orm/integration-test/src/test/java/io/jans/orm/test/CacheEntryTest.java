/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.test.model.SimpleCacheEntry;

/**
 * Native persistence cache entry create/update/update with removed value.
 * It's automated version of SqlUpateCacheEntrySample/SpannerUpateCacheEntrySample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class CacheEntryTest extends BaseOrmTest {

	private String cacheDn;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (cacheDn != null)) {
			try {
				entryManager.remove(cacheDn, SimpleCacheEntry.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createAndUpdateCacheEntry() {
		String key = UUID.randomUUID().toString();
		cacheDn = String.format("uuid=%s,%s", key, "ou=cache,o=jans");

		int expirationInSeconds = 60;
		Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(new Date());
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

		SimpleCacheEntry entity = new SimpleCacheEntry();
		entity.setTtl(expirationInSeconds);
		entity.setData("sample_data");
		entity.setId(key);
		entity.setDn(cacheDn);
		entity.setCreationDate(new Date());
		entity.setExpirationDate(expirationDate.getTime());
		entity.setDeletable(true);

		entryManager.persist(entity);

		SimpleCacheEntry foundEntity = entryManager.find(SimpleCacheEntry.class, cacheDn);
		assertNotNull(foundEntity);
		assertEquals(foundEntity.getData(), "sample_data");

		// Update
		entity.setData("updated_data");
		entryManager.merge(entity);

		foundEntity = entryManager.find(SimpleCacheEntry.class, cacheDn);
		assertEquals(foundEntity.getData(), "updated_data");

		// Update with removed value
		entity.setData(null);
		entryManager.merge(entity);

		foundEntity = entryManager.find(SimpleCacheEntry.class, cacheDn);
		assertNull(foundEntity.getData());
	}

}
