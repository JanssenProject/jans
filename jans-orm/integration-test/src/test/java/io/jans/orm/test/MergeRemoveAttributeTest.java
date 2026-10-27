/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.test.model.SessionId;
import io.jans.orm.test.model.SimpleCacheEntry;
import io.jans.orm.test.model.SimpleUser;

/**
 * Verifies that merge() clears attribute values in DB. Covers every branch of
 * BaseEntryManager.collectAttributeModifications which produces REMOVE modification
 * and checks that SqlEntryManager/SpannerEntryManager.merge() translate it to
 * "set column to NULL":
 *
 * 1. forceUpdate entity, property is null (DB state unknown, REMOVE with null old value)
 * 2. forceUpdate entity, property is empty string (REMOVE with empty old value)
 * 3. forceUpdate entity, REMOVE for already empty cell should not fail
 * 4. regular entity, property is null (REMOVE with old value loaded from DB)
 * 5. regular entity, property is empty string (REMOVE with old value loaded from DB)
 * 6. regular entity, REMOVE for already empty cell should be skipped and not fail
 * 7. regular entity, multi-valued property set to empty list and to null
 * 8. regular entity, @AttributesList attribute set to empty list
 *
 * @author Yuriy Movchan Date: 09/10/2026
 */
public class MergeRemoveAttributeTest extends BaseOrmTest {

	private static final String CACHE_BASE_DN = "ou=cache,o=jans";
	private static final String SESSIONS_BASE_DN = "ou=sessions,o=jans";
	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private SimpleCacheEntry cacheEntry;
	private SessionId sessionId;
	private SimpleUser user;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		if (cacheEntry != null) {
			try {
				entryManager.remove(cacheEntry.getDn(), SimpleCacheEntry.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
		if (sessionId != null) {
			try {
				entryManager.remove(sessionId.getDn(), SessionId.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
		if (user != null) {
			try {
				entryManager.remove(user.getDn(), SimpleUser.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	/*
	 * forceUpdate = true entity. ORM does not load entry from DB before merge,
	 * hence old attribute value is always unknown and REMOVE must set NULL.
	 */

	@Test
	public void forceUpdateClearValueWithNull() {
		cacheEntry = buildCacheEntry("sample_data");
		entryManager.persist(cacheEntry);

		SimpleCacheEntry found = entryManager.find(SimpleCacheEntry.class, cacheEntry.getDn());
		assertEquals(found.getData(), "sample_data");

		cacheEntry.setData(null);
		entryManager.merge(cacheEntry);

		found = entryManager.find(SimpleCacheEntry.class, cacheEntry.getDn());
		assertNull(found.getData());
	}

	@Test(dependsOnMethods = "forceUpdateClearValueWithNull")
	public void forceUpdateMergeNullOnAlreadyEmptyValue() {
		// Column is already NULL in DB. REMOVE reaches DB as SET NULL and must not fail
		cacheEntry.setData(null);
		entryManager.merge(cacheEntry);

		SimpleCacheEntry found = entryManager.find(SimpleCacheEntry.class, cacheEntry.getDn());
		assertNull(found.getData());
	}

	@Test(dependsOnMethods = "forceUpdateMergeNullOnAlreadyEmptyValue")
	public void forceUpdateRestoreValueAfterClear() {
		cacheEntry.setData("restored_data");
		entryManager.merge(cacheEntry);

		SimpleCacheEntry found = entryManager.find(SimpleCacheEntry.class, cacheEntry.getDn());
		assertEquals(found.getData(), "restored_data");
	}

	@Test(dependsOnMethods = "forceUpdateRestoreValueAfterClear")
	public void forceUpdateClearValueWithEmptyString() {
		cacheEntry.setData("");
		entryManager.merge(cacheEntry);

		SimpleCacheEntry found = entryManager.find(SimpleCacheEntry.class, cacheEntry.getDn());
		assertTrue(isEmpty(found.getData()), "Expected empty value but found: " + found.getData());
	}

	/*
	 * Regular entity. ORM loads entry from DB before merge and compares values.
	 */

	@Test
	public void regularClearValueWithNull() {
		sessionId = buildSessionId();
		entryManager.persist(sessionId);

		SessionId found = entryManager.find(SessionId.class, sessionId.getDn());
		assertEquals(found.getJwt(), "{}");
		assertEquals(found.getSessionState(), "session_state");

		sessionId.setJwt(null);
		entryManager.merge(sessionId);

		found = entryManager.find(SessionId.class, sessionId.getDn());
		assertNull(found.getJwt());
		// Other attributes are not affected
		assertEquals(found.getSessionState(), "session_state");
		assertEquals(found.getId(), sessionId.getId());
	}

	@Test(dependsOnMethods = "regularClearValueWithNull")
	public void regularMergeNullOnAlreadyEmptyValue() {
		// Column is already NULL in DB. ORM should skip REMOVE for it and merge must not fail
		sessionId.setJwt(null);
		sessionId.setLastUsedAt(new Date());
		entryManager.merge(sessionId);

		SessionId found = entryManager.find(SessionId.class, sessionId.getDn());
		assertNull(found.getJwt());
		assertNotNull(found.getLastUsedAt());
	}

	@Test(dependsOnMethods = "regularMergeNullOnAlreadyEmptyValue")
	public void regularRestoreValueAfterClear() {
		sessionId.setJwt("{\"restored\":true}");
		entryManager.merge(sessionId);

		SessionId found = entryManager.find(SessionId.class, sessionId.getDn());
		assertEquals(found.getJwt(), "{\"restored\":true}");
	}

	@Test(dependsOnMethods = "regularRestoreValueAfterClear")
	public void regularClearValueWithEmptyString() {
		sessionId.setJwt("");
		entryManager.merge(sessionId);

		SessionId found = entryManager.find(SessionId.class, sessionId.getDn());
		assertTrue(isEmpty(found.getJwt()), "Expected empty value but found: " + found.getJwt());
	}

	@Test(dependsOnMethods = "regularClearValueWithEmptyString")
	public void regularClearBooleanWithNull() {
		assertEquals(entryManager.find(SessionId.class, sessionId.getDn()).getPermissionGranted(), Boolean.TRUE);

		sessionId.setPermissionGranted(null);
		entryManager.merge(sessionId);

		SessionId found = entryManager.find(SessionId.class, sessionId.getDn());
		assertNull(found.getPermissionGranted());
	}

	/*
	 * Regular entity with multi-valued attributes
	 */

	@Test
	public void regularClearMultiValuedWithEmptyList() {
		user = buildUser();
		entryManager.persist(user);

		SimpleUser found = entryManager.find(SimpleUser.class, user.getDn());
		assertEquals(found.getMemberOf().size(), 2);
		assertEquals(found.getAttributeValues("jansOptOuts").size(), 3);

		user.setMemberOf(new ArrayList<String>());
		user.setAttributeValues("jansOptOuts", new ArrayList<Object>());
		entryManager.merge(user);

		found = entryManager.find(SimpleUser.class, user.getDn());
		assertTrue(isEmpty(found.getMemberOf()), "Expected empty memberOf but found: " + found.getMemberOf());
		assertTrue(isEmpty(found.getAttributeValues("jansOptOuts")), "Expected empty jansOptOuts but found: " + found.getAttributeValues("jansOptOuts"));
		// Other attributes are not affected
		assertEquals(found.getUserId(), user.getUserId());
		assertEquals(String.valueOf(found.getAttribute("jansGuid")), "test_guid");
	}

	@Test(dependsOnMethods = "regularClearMultiValuedWithEmptyList")
	public void regularRestoreMultiValuedAfterClear() {
		user.setMemberOf(Arrays.asList("inum=group3,ou=groups,o=jans"));
		user.setAttributeValues("jansOptOuts", Arrays.asList("Dublin"));
		entryManager.merge(user);

		SimpleUser found = entryManager.find(SimpleUser.class, user.getDn());
		assertEquals(found.getMemberOf(), Arrays.asList("inum=group3,ou=groups,o=jans"));
		assertEquals(found.getAttributeValues("jansOptOuts"), Arrays.asList("Dublin"));
	}

	@Test(dependsOnMethods = "regularRestoreMultiValuedAfterClear")
	public void regularClearMultiValuedWithNull() {
		user.setMemberOf(null);
		entryManager.merge(user);

		SimpleUser found = entryManager.find(SimpleUser.class, user.getDn());
		assertTrue(isEmpty(found.getMemberOf()), "Expected empty memberOf but found: " + found.getMemberOf());
		assertEquals(found.getAttributeValues("jansOptOuts"), Arrays.asList("Dublin"));
	}

	private SimpleCacheEntry buildCacheEntry(String data) {
		String key = UUID.randomUUID().toString();

		int expirationInSeconds = 120;
		Calendar expirationDate = Calendar.getInstance();
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

		SimpleCacheEntry entity = new SimpleCacheEntry();
		entity.setDn(String.format("uuid=%s,%s", key, CACHE_BASE_DN));
		entity.setId(key);
		entity.setTtl(expirationInSeconds);
		entity.setData(data);
		entity.setCreationDate(new Date());
		entity.setExpirationDate(expirationDate.getTime());
		entity.setDeletable(true);

		return entity;
	}

	private SessionId buildSessionId() {
		SessionId entity = new SessionId();
		entity.setId(UUID.randomUUID().toString());
		entity.setDn(String.format("jansId=%s,%s", entity.getId(), SESSIONS_BASE_DN));
		entity.setCreationDate(new Date());
		entity.setJwt("{}");
		entity.setIsJwt(true);
		entity.setSessionState("session_state");
		entity.setPermissionGranted(true);

		Calendar expirationDate = Calendar.getInstance();
		expirationDate.add(Calendar.SECOND, 120);
		entity.setExpirationDate(expirationDate.getTime());
		entity.setTtl(120);

		return entity;
	}

	private SimpleUser buildUser() {
		String inum = getRandomInum();

		SimpleUser entity = new SimpleUser();
		entity.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
		entity.setUserId("merge_remove_user_" + inum);
		entity.setMemberOf(Arrays.asList("inum=group1,ou=groups,o=jans", "inum=group2,ou=groups,o=jans"));
		entity.getCustomAttributes().add(new CustomObjectAttribute("jansOptOuts", Arrays.asList("London", "Texas", "Kiev")));
		entity.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_guid"));

		return entity;
	}

	private boolean isEmpty(String value) {
		return (value == null) || value.isEmpty();
	}

	private boolean isEmpty(List<?> values) {
		return (values == null) || values.isEmpty();
	}

}
