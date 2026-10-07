/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SessionId;
import io.jans.orm.util.Pair;

/**
 * Session entry CRUD, search with ranges, paged search, delete and count by filter,
 * batch job. It's automated version of manual samples and ManualSqlEntryManagerTest.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class SessionIdCrudTest extends BaseOrmTest {

	private static final String SESSIONS_BASE_DN = "ou=sessions,o=jans";

	private SessionId persistedSessionId;
	private int totalProcessedCount;

	private List<String> usedOutsideSids = new ArrayList<>();

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		for (String outsideSid : usedOutsideSids) {
			try {
				entryManager.remove(SESSIONS_BASE_DN, SessionId.class, Filter.createEqualityFilter("sid", outsideSid), 1000);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createSessionId() {
		SessionId sessionId = buildSessionId();
		entryManager.persist(sessionId);

		persistedSessionId = sessionId;
	}

	@Test(dependsOnMethods = "createSessionId")
	public void searchSessionId() {
		Filter filter = Filter.createEqualityFilter("jansId", persistedSessionId.getId());
		List<SessionId> sessionIdList = entryManager.findEntries("o=jans", SessionId.class, filter);

		assertNotNull(sessionIdList);
		assertEquals(sessionIdList.size(), 1);
		assertEquals(sessionIdList.get(0).getId(), persistedSessionId.getId());
	}

	@Test(dependsOnMethods = "searchSessionId")
	public void containsSessionId() {
		boolean result = entryManager.contains(persistedSessionId);

		assertTrue(result);
	}

	@Test(dependsOnMethods = "containsSessionId")
	public void updateSessionId() {
		SessionId sessionId = persistedSessionId;

		sessionId.setAuthenticationTime(new Date());
		sessionId.setLastUsedAt(new Date());

		sessionId.setJwt(null);
		sessionId.setIsJwt(null);

		Pair<Date, Integer> expiration = expirationDate(new Date());
		sessionId.setExpirationDate(expiration.getFirst());
		sessionId.setTtl(expiration.getSecond());

		entryManager.merge(sessionId);

		SessionId updatedSessionId = entryManager.find(SessionId.class, sessionId.getDn());
		assertNotNull(updatedSessionId.getAuthenticationTime());
		assertNotNull(updatedSessionId.getLastUsedAt());
	}

	@Test(dependsOnMethods = "updateSessionId")
	public void countSessionId() {
		int countEntries = entryManager.countEntries(persistedSessionId);

		assertEquals(countEntries, 1);
	}

	@Test(dependsOnMethods = "countSessionId")
	public void containsSessionIdAfterUpdate() {
		boolean result = entryManager.contains(persistedSessionId);

		assertTrue(result);
	}

	@Test(dependsOnMethods = "containsSessionIdAfterUpdate")
	public void deleteSessionId() {
		entryManager.remove(persistedSessionId);

		boolean result = entryManager.contains(persistedSessionId);

		assertFalse(result);
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void searchSessionIdWithRanges() {
		String outsideSid = buildSessionIds(20);

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		List<SessionId> sessionIdList = entryManager.findEntries("o=jans", SessionId.class, filter);
		assertNotNull(sessionIdList);
		assertEquals(sessionIdList.size(), 20);

		List<SessionId> sessionIdList2 = entryManager.findEntries("o=jans", SessionId.class, filter, 5);
		assertNotNull(sessionIdList2);
		assertEquals(sessionIdList2.size(), 5);

		List<SessionId> sessionIdList3 = entryManager.findEntries("o=jans", SessionId.class, filter, 25);
		assertNotNull(sessionIdList3);
		assertEquals(sessionIdList3.size(), 20);

		List<SessionId> sessionIdList4 = entryManager.findEntries("o=jans", SessionId.class, filter, null, null, 14, 7, 3);
		assertNotNull(sessionIdList4);
		assertEquals(sessionIdList4.size(), 6);

		List<SessionId> sessionIdList5 = entryManager.findEntries("o=jans", SessionId.class, filter, null, null, 20, 10, 5);
		assertNotNull(sessionIdList5);
		assertEquals(sessionIdList5.size(), 0);

		List<SessionId> sessionIdList6 = entryManager.findEntries("o=jans", SessionId.class, filter, null, null, 19, -1, 5);
		assertNotNull(sessionIdList6);
		assertEquals(sessionIdList6.size(), 1);
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void searchPagedSessionIdWithRanges() {
		String outsideSid = buildSessionIds(20);

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		PagedResult<SessionId> sessionIdList = entryManager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 0, -1, -1);
		assertNotNull(sessionIdList);
		assertEquals(sessionIdList.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList2 = entryManager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 0, 5, -1);
		assertNotNull(sessionIdList2);
		assertEquals(sessionIdList2.getStart(), 0);
		assertEquals(sessionIdList2.getEntriesCount(), 5);
		assertEquals(sessionIdList2.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList4 = entryManager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 14, 7, 3);
		assertNotNull(sessionIdList4);
		assertEquals(sessionIdList4.getStart(), 14);
		assertEquals(sessionIdList4.getEntriesCount(), 6);
		assertEquals(sessionIdList4.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList5 = entryManager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 20, 10, 5);
		assertNotNull(sessionIdList5);
		assertEquals(sessionIdList5.getStart(), 20);
		assertEquals(sessionIdList5.getEntriesCount(), 0);
		assertEquals(sessionIdList5.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList6 = entryManager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 19, -1, 5);
		assertNotNull(sessionIdList6);
		assertEquals(sessionIdList6.getStart(), 19);
		assertEquals(sessionIdList6.getEntriesCount(), 1);
		assertEquals(sessionIdList6.getTotalEntriesCount(), 20);
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void deleteSessionIdByFilter() {
		String outsideSid = buildSessionIds(20);

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		int removedCount = entryManager.remove(SESSIONS_BASE_DN, SessionId.class, filter, 14);
		assertEquals(removedCount, 14);

		int removedCount2 = entryManager.remove(SESSIONS_BASE_DN, SessionId.class, filter, 5);
		assertEquals(removedCount2, 5);

		int removedCount3 = entryManager.remove(SESSIONS_BASE_DN, SessionId.class, filter, 1);
		assertEquals(removedCount3, 1);
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void countSessionIdByFilter() {
		String outsideSid = buildSessionIds(33);

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		int countEntries = entryManager.countEntries(SESSIONS_BASE_DN, SessionId.class, filter, null);
		assertEquals(countEntries, 33);
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void updateMissingSessionId() {
		SessionId sessionId = buildSessionId();

		try {
			entryManager.merge(sessionId);
			assertFalse(true, "Merge of missing entry should throw EntryPersistenceException");
		} catch (EntryPersistenceException ex) {
			// Expected
		}
	}

	@Test(dependsOnMethods = "deleteSessionId")
	public void batchJob() {
		String outsideSid = UUID.randomUUID().toString();
		usedOutsideSids.add(outsideSid);
		for (int i = 0; i < 200; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			Pair<Date, Integer> expiration = expirationDate(new Date());
			sessionId.setExpirationDate(expiration.getFirst());
			sessionId.setTtl(expiration.getSecond());

			entryManager.persist(sessionId);
		}

		totalProcessedCount = 0;

		ProcessBatchOperation<SessionId> sessionBatchOperation = new ProcessBatchOperation<SessionId>() {
			@Override
			public void performAction(List<SessionId> objects) {
				for (SessionId sessionId : objects) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(sessionId.getExpirationDate());
					calendar.add(Calendar.SECOND, -1);

					sessionId.setExpirationDate(calendar.getTime());

					entryManager.merge(sessionId);
					totalProcessedCount++;
				}
			}
		};

		Filter filter = Filter.createANDFilter(Filter.createPresenceFilter("exp"), Filter.createEqualityFilter("sid", outsideSid));
		entryManager.findEntries("o=jans", SessionId.class, filter, SearchScope.SUB, new String[] { "exp", "sid" },
				sessionBatchOperation, 0, 500, 100);

		assertEquals(totalProcessedCount, 200);
	}

	private String buildSessionIds(int count) {
		String outsideSid = UUID.randomUUID().toString();
		usedOutsideSids.add(outsideSid);

		for (int i = 0; i < count; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			entryManager.persist(sessionId);
		}

		return outsideSid;
	}

	private SessionId buildSessionId() {
		SessionId sessionId = new SessionId();
		sessionId.setId(UUID.randomUUID().toString());
		sessionId.setDn(String.format("jansId=%s,%s", sessionId.getId(), SESSIONS_BASE_DN));
		sessionId.setCreationDate(new Date());
		sessionId.setJwt("{}");
		sessionId.setIsJwt(true);

		return sessionId;
	}

	private Pair<Date, Integer> expirationDate(Date creationDate) {
		int expirationInSeconds = 120;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(creationDate);
		calendar.add(Calendar.SECOND, expirationInSeconds);
		return new Pair<>(calendar.getTime(), expirationInSeconds);
	}

}
