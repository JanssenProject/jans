/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SessionId;

/**
 * Removal of expired sessions by filter with count limit like cleaner services do.
 * Test creates expired and not expired sessions, counts them, removes expired ones
 * in 2 iterations and checks that not expired sessions are left intact. It's
 * automated version of SqlDeleteSample/CouchbaseDeleteSample/LdapDeleteSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class DeleteExpiredSessionsTest extends BaseOrmTest {

	private static final String SESSIONS_BASE_DN = "ou=sessions,o=jans";

	private static final int EXPIRED_SESSIONS_COUNT = 8;
	private static final int NOT_EXPIRED_SESSIONS_COUNT = 4;

	private String marker;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager == null) || (marker == null)) {
			return;
		}

		try {
			entryManager.remove(SESSIONS_BASE_DN, SessionId.class, markerFilter(), 1000);
		} catch (Exception ex) {
			// Test cleanup should not fail test run
		}
	}

	@Test
	public void createExpiredAndActiveSessions() {
		marker = UUID.randomUUID().toString();

		// Expired sessions: expiration date in the past
		for (int i = 0; i < EXPIRED_SESSIONS_COUNT; i++) {
			entryManager.persist(buildSessionId(-1));
		}

		// Not expired sessions: expiration date in the future
		for (int i = 0; i < NOT_EXPIRED_SESSIONS_COUNT; i++) {
			entryManager.persist(buildSessionId(1));
		}

		assertEquals(entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, markerFilter()),
				EXPIRED_SESSIONS_COUNT + NOT_EXPIRED_SESSIONS_COUNT);
	}

	@Test(dependsOnMethods = "createExpiredAndActiveSessions")
	public void countExpiredSessions() {
		int expiredCount = entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, expiredFilter());

		assertEquals(expiredCount, EXPIRED_SESSIONS_COUNT);
	}

	@Test(dependsOnMethods = "countExpiredSessions")
	public void removeExpiredSessionsWithLimit() {
		// First iteration removes not more than 5 expired sessions
		int removedCount = entryManager.remove(SESSIONS_BASE_DN, SessionId.class, expiredFilter(), 5);
		assertEquals(removedCount, 5);

		int leftExpiredCount = entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, expiredFilter());
		assertEquals(leftExpiredCount, EXPIRED_SESSIONS_COUNT - 5);

		// Second iteration removes the rest of expired sessions
		int removedCount2 = entryManager.remove(SESSIONS_BASE_DN, SessionId.class, expiredFilter(), 5);
		assertEquals(removedCount2, EXPIRED_SESSIONS_COUNT - 5);

		int leftExpiredCount2 = entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, expiredFilter());
		assertEquals(leftExpiredCount2, 0);
	}

	@Test(dependsOnMethods = "removeExpiredSessionsWithLimit")
	public void checkNotExpiredSessionsLeftIntact() {
		int leftCount = entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, markerFilter());

		assertEquals(leftCount, NOT_EXPIRED_SESSIONS_COUNT);
	}

	private Filter markerFilter() {
		return Filter.createEqualityFilter("sid", marker);
	}

	private Filter expiredFilter() {
		return Filter.createANDFilter(
				markerFilter(),
				Filter.createEqualityFilter("del", true),
				Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(SESSIONS_BASE_DN, new Date())));
	}

	private SessionId buildSessionId(int expirationInHours) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, expirationInHours);

		SessionId sessionId = new SessionId();
		sessionId.setId(UUID.randomUUID().toString());
		sessionId.setDn(String.format("jansId=%s,%s", sessionId.getId(), SESSIONS_BASE_DN));
		sessionId.setOutsideSid(marker);
		sessionId.setCreationDate(new Date());
		sessionId.setExpirationDate(calendar.getTime());
		sessionId.setDeletable(true);

		return sessionId;
	}

}
