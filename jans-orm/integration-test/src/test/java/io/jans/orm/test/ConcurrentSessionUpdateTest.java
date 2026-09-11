/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.test.model.SimpleSessionState;

/**
 * Concurrent update of the same session entry from multiple threads.
 * It's automated version of *ConcurentSessionUpdateSample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class ConcurrentSessionUpdateTest extends BaseOrmTest {

	private static final int THREAD_COUNT = 20;
	private static final int UPDATES_PER_THREAD = 5;

	private String sessionDn;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (sessionDn != null)) {
			try {
				entryManager.remove(sessionDn, SimpleSessionState.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void concurrentSessionUpdate() throws InterruptedException {
		String sessionId = UUID.randomUUID().toString();
		sessionDn = String.format("jansId=%s,ou=sessions,o=jans", sessionId);
		final String userDn = "inum=test_user,ou=people,o=jans";

		SimpleSessionState simpleSessionState = new SimpleSessionState();
		simpleSessionState.setDn(sessionDn);
		simpleSessionState.setId(sessionId);
		simpleSessionState.setLastUsedAt(new Date());

		entryManager.persist(simpleSessionState);

		final AtomicInteger successCount = new AtomicInteger();
		final AtomicInteger errorCount = new AtomicInteger();

		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			final int threadIndex = i;
			executorService.execute(() -> {
				for (int j = 0; j < UPDATES_PER_THREAD; j++) {
					try {
						SimpleSessionState sessionFromDb = entryManager.find(SimpleSessionState.class, sessionDn);

						sessionFromDb.setUserDn((threadIndex % 2 == 0) ? userDn : "");
						sessionFromDb.setLastUsedAt(new Date());

						entryManager.merge(sessionFromDb);
						successCount.incrementAndGet();
					} catch (Throwable ex) {
						errorCount.incrementAndGet();
					}
				}
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(120, TimeUnit.SECONDS);

		assertEquals(errorCount.get(), 0, "Concurrent updates failed");
		assertEquals(successCount.get(), THREAD_COUNT * UPDATES_PER_THREAD);

		SimpleSessionState finalSessionState = entryManager.find(SimpleSessionState.class, sessionDn);
		assertNotNull(finalSessionState);
		assertNotNull(finalSessionState.getLastUsedAt());
	}

}
