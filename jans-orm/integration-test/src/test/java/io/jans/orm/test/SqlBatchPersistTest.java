/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.InvalidArgumentException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.test.model.SessionId;
import io.jans.orm.test.model.SessionIdState;
import io.jans.orm.test.model.SimpleBinaryEntry;
import io.jans.orm.test.model.SimpleClient;
import io.jans.orm.test.model.VersionedTestEntry;

/**
 * Atomic batch insert ({@code persistBatch}) and multi-get ({@code findByDns}) of the SQL backend
 * against a real database.
 *
 * <p>Commit counting uses PostgreSQL statistics: {@code pg_stat_database.xact_commit} (all committed
 * transactions of the database, read-only autocommit statements included) and the next transaction id
 * {@code txid_snapshot_xmax(txid_current_snapshot())}, which advances once per write transaction.
 * PostgreSQL 15+ flushes backend statistics to shared memory at most once per second and an idle
 * backend can delay it up to 10 s, so {@code xact_commit} is read after {@link #STATS_SETTLE_MILLIS}.
 * The database is shared with other services of the stand, so both counters get commits of other
 * sessions. An idle window of the same length is measured first and subtracted; the remaining
 * variation of that noise, the pool validation query and the probe itself are covered by
 * {@link #WRITE_NOISE_TOLERANCE} and {@link #XACT_NOISE_TOLERANCE}. The write transaction count is the
 * exact signal (1 for the batch, one per entry for single persist); {@code xact_commit} also counts
 * read-only statements of other sessions, so its bound is looser.</p>
 *
 * @author Yuriy Movchan Date: 11/12/2026
 */
public class SqlBatchPersistTest extends BaseOrmTest {

	private static final Logger LOG = LoggerFactory.getLogger(SqlBatchPersistTest.class);

	private static final String SESSIONS_BASE_DN = "ou=sessions,o=jans";
	private static final String CLIENTS_BASE_DN = "ou=clients,o=jans";
	private static final String VERSION_TEST_BASE_DN = "ou=version_test,o=jans";
	private static final String BINARY_BASE_DN = "ou=binary,o=jans";

	/** Write transactions of other sessions tolerated per measurement (jans-dev: 0-1 per 11 s window) */
	private static final int WRITE_NOISE_TOLERANCE = 10;
	/**
	 * Read-only commits of other sessions tolerated per measurement on top of the idle-window baseline
	 * (jans-dev: 16-34 per 11 s window, so the baseline varies by up to about 20)
	 */
	private static final int XACT_NOISE_TOLERANCE = 30;
	private static final long STATS_SETTLE_MILLIS = 11000L;

	private String marker;

	private final Set<String> createdClientDns = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<String> createdVersionedDns = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<String> createdBinaryDns = Collections.synchronizedSet(new LinkedHashSet<>());

	@BeforeClass
	public void checkPersistenceType() {
		requirePersistenceType(SQL);
		marker = "batch_" + UUID.randomUUID().toString().replace("-", "");
	}

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		// All test sessions carry the marker in "sid"
		try {
			entryManager.remove(SESSIONS_BASE_DN, SessionId.class, Filter.createEqualityFilter("sid", marker), 10000);
		} catch (Exception ex) {
			LOG.warn("Failed to remove test sessions", ex);
		}

		removeQuietly(createdClientDns, SimpleClient.class);
		removeQuietly(createdVersionedDns, VersionedTestEntry.class);
		removeQuietly(createdBinaryDns, SimpleBinaryEntry.class);
	}

	@Test
	public void capabilityIsReported() {
		assertTrue(entryManager.isAtomicBatchSupported());
		assertTrue(entryManager.isAtomicBatchSupported(SESSIONS_BASE_DN));
	}

	@Test
	public void emptyBatchIsNoOp() {
		entryManager.persistBatch(Collections.emptyList());
	}

	@Test
	public void nullElementIsRejectedAndNothingStored() {
		SessionId session = buildSession();
		try {
			entryManager.persistBatch(Arrays.asList(session, null));
			fail("Expected MappingException for a null element");
		} catch (MappingException ex) {
			// expected
		}

		assertFalse(entryManager.contains(session.getDn(), SessionId.class));
	}

	@Test
	public void batchOverLimitIsRejected() {
		List<SessionId> sessions = new ArrayList<>();
		for (int i = 0; i <= SqlOperationService.MAX_BATCH_INSERT_SIZE; i++) {
			sessions.add(buildSession());
		}

		try {
			entryManager.persistBatch(sessions);
			fail("Expected InvalidArgumentException for a batch over the limit");
		} catch (InvalidArgumentException ex) {
			// expected
		}

		assertFalse(entryManager.contains(sessions.get(0).getDn(), SessionId.class));
	}

	@Test
	public void batchOfOneClassStoresSameValuesAsPersist() {
		SessionId single = buildSession();
		entryManager.persist(single);

		List<SessionId> batch = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			SessionId session = buildSession();
			copySessionValues(single, session);
			batch.add(session);
		}

		entryManager.persistBatch(batch);

		SessionId foundSingle = entryManager.find(SessionId.class, single.getDn());
		for (SessionId session : batch) {
			SessionId found = entryManager.find(SessionId.class, session.getDn());
			assertEquals(found.getId(), session.getId());
			assertSessionValuesEqual(found, foundSingle);
		}
	}

	@Test
	public void batchOfTwoClassesStoresBoth() {
		List<Object> batch = new ArrayList<>();
		List<SessionId> sessions = new ArrayList<>();
		List<SimpleClient> clients = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			SessionId session = buildSession();
			SimpleClient client = buildClient();
			sessions.add(session);
			clients.add(client);
			// Interleaved: one statement per entry, the list order is kept
			batch.add(session);
			batch.add(client);
		}

		entryManager.persistBatch(batch);

		for (SessionId session : sessions) {
			assertEquals(entryManager.find(SessionId.class, session.getDn()).getId(), session.getId());
		}
		for (SimpleClient client : clients) {
			SimpleClient found = entryManager.find(SimpleClient.class, client.getDn());
			assertEquals(found.getClientName(), client.getClientName());
			assertEquals(found.getDefaultAcrValues(), client.getDefaultAcrValues());
		}
	}

	@Test
	public void duplicateOfExistingEntryRollsBackWholeBatch() {
		SessionId existing = buildSession();
		existing.setJwt("existing");
		entryManager.persist(existing);

		List<SessionId> batch = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			batch.add(buildSession());
		}
		SessionId duplicate = buildSession();
		duplicate.setId(existing.getId());
		duplicate.setDn(existing.getDn());
		duplicate.setJwt("duplicate");
		batch.add(duplicate);

		DuplicateEntryException duplicateException = expectDuplicate(batch);
		assertEquals(duplicateException.getDn(), existing.getDn());

		assertNoneStored(batch.subList(0, 10));
		assertEquals(entryManager.find(SessionId.class, existing.getDn()).getJwt(), "existing");
	}

	@Test
	public void singlePersistOfExistingDnHasDuplicateCause() {
		SessionId existing = buildSession();
		existing.setJwt("existing");
		entryManager.persist(existing);

		SessionId duplicate = buildSession();
		duplicate.setId(existing.getId());
		duplicate.setDn(existing.getDn());
		duplicate.setJwt("duplicate");

		try {
			entryManager.persist(duplicate);
			fail("Expected EntryPersistenceException caused by DuplicateEntryException");
		} catch (EntryPersistenceException ex) {
			assertTrue(ex.getCause() instanceof DuplicateEntryException, String.valueOf(ex.getCause()));
			assertEquals(((DuplicateEntryException) ex.getCause()).getDn(), existing.getDn());
		}

		assertEquals(entryManager.find(SessionId.class, existing.getDn()).getJwt(), "existing");
	}

	@Test
	public void sameDnTwiceInBatchStoresNothing() {
		List<SessionId> batch = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			batch.add(buildSession());
		}
		SessionId repeated = buildSession();
		repeated.setId(batch.get(2).getId());
		repeated.setDn(batch.get(2).getDn());
		batch.add(repeated);

		DuplicateEntryException duplicateException = expectDuplicate(batch);
		assertEquals(duplicateException.getDn(), repeated.getDn());

		assertNoneStored(batch);
	}

	@Test
	public void versionedEntryIsStoredWithInitialVersion() {
		List<VersionedTestEntry> batch = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			VersionedTestEntry entry = new VersionedTestEntry();
			entry.setDn(String.format("uid=%s,%s", UUID.randomUUID(), VERSION_TEST_BASE_DN));
			entry.setUid(marker + "_" + i);
			entry.setData("initial");
			batch.add(entry);
			createdVersionedDns.add(entry.getDn());
		}

		entryManager.persistBatch(batch);

		VersionedTestEntry single = new VersionedTestEntry();
		single.setDn(String.format("uid=%s,%s", UUID.randomUUID(), VERSION_TEST_BASE_DN));
		single.setUid(marker + "_single");
		single.setData("initial");
		entryManager.persist(single);
		createdVersionedDns.add(single.getDn());

		for (VersionedTestEntry entry : batch) {
			assertEquals(entry.getVersion(), single.getVersion());

			VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
			assertEquals(found.getVersion(), single.getVersion());

			found.setData("updated");
			entryManager.updateWithVersion(found);

			VersionedTestEntry updated = entryManager.find(VersionedTestEntry.class, entry.getDn());
			assertEquals(updated.getData(), "updated");
			assertEquals(updated.getVersion().longValue(), single.getVersion() + 1);
		}
	}

	@Test
	public void jsonMultivaluedAndTimeAttributesRoundTrip() {
		SessionId session = buildSession();
		Map<String, String> sessionAttributes = new HashMap<>();
		sessionAttributes.put("acr", "basic");
		sessionAttributes.put("quoted", "a \"b\" c");
		session.setSessionAttributes(sessionAttributes);

		SimpleClient client = buildClient();
		client.setDefaultAcrValues(new String[] { "basic", "otp", "with space" });

		entryManager.persistBatch(Arrays.asList(session, client));

		SessionId foundSession = entryManager.find(SessionId.class, session.getDn());
		assertEquals(foundSession.getSessionAttributes(), sessionAttributes);
		assertEquals(foundSession.getCreationDate(), session.getCreationDate());
		assertEquals(foundSession.getLastUsedAt(), session.getLastUsedAt());
		assertEquals(foundSession.getAuthenticationTime(), session.getAuthenticationTime());
		assertEquals(foundSession.getExpirationDate(), session.getExpirationDate());

		SimpleClient foundClient = entryManager.find(SimpleClient.class, client.getDn());
		assertEquals(foundClient.getDefaultAcrValues(), client.getDefaultAcrValues());
	}

	@Test
	public void binaryAttributesRoundTrip() {
		Map<String, ?> binaryTable = entryManager.getTableColumnsMap().get("jansbinentry");
		if (binaryTable == null) {
			throw new SkipException("DB has no jansBinEntry table. See BinaryDataTest javadoc for DDL");
		}

		byte[] binaryData = new byte[1024];
		new SecureRandom().nextBytes(binaryData);

		List<SimpleBinaryEntry> batch = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			SimpleBinaryEntry entry = new SimpleBinaryEntry();
			entry.setDn(String.format("inum=%s,%s", getRandomInum(), BINARY_BASE_DN));
			entry.setDisplayName(marker);
			entry.setDataBin(binaryData);
			entry.setDataStr(binaryData);
			batch.add(entry);
			createdBinaryDns.add(entry.getDn());
		}

		entryManager.persistBatch(batch);

		for (SimpleBinaryEntry entry : batch) {
			SimpleBinaryEntry found = entryManager.find(SimpleBinaryEntry.class, entry.getDn());
			assertEquals(found.getDataBin(), binaryData);
			assertEquals(found.getDataStr(), binaryData);
		}
	}

	@Test
	public void oneCommitPerBatch() throws Exception {
		if (!(entryManager instanceof SqlEntryManager)) {
			throw new SkipException("Commit counting needs direct SQL entry manager");
		}
		SqlEntryManager sqlEntryManager = (SqlEntryManager) entryManager;
		if (!sqlEntryManager.getOperationService().getMetadata().getDatabaseProductName().toLowerCase().contains("postgres")) {
			throw new SkipException("Commit counting uses PostgreSQL statistics");
		}

		int count = 100;
		List<SessionId> batch = new ArrayList<>();
		List<SessionId> singles = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			batch.add(buildSession());
			singles.add(buildSession());
		}

		// Idle window of the same length measures the commits of other sessions
		long[] beforeIdle = readCommitCounters(sqlEntryManager);
		long[] beforeBatch = readCommitCounters(sqlEntryManager);
		long noiseXactCommits = beforeBatch[0] - beforeIdle[0];
		long noiseWriteTransactions = beforeBatch[1] - beforeIdle[1];

		long batchStart = System.nanoTime();
		entryManager.persistBatch(batch);
		long batchMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - batchStart);
		long[] afterBatch = readCommitCounters(sqlEntryManager);

		long singlesStart = System.nanoTime();
		for (SessionId session : singles) {
			entryManager.persist(session);
		}
		long singlesMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - singlesStart);
		long[] afterSingles = readCommitCounters(sqlEntryManager);

		long batchXactCommits = afterBatch[0] - beforeBatch[0];
		long batchWriteTransactions = afterBatch[1] - beforeBatch[1];
		long singlesXactCommits = afterSingles[0] - afterBatch[0];
		long singlesWriteTransactions = afterSingles[1] - afterBatch[1];

		String report = String.format("%d entries: idle window xact_commit delta %d, write transactions %d; "
				+ "persistBatch %d ms, xact_commit delta %d, write transactions %d; "
				+ "single persist %d ms, xact_commit delta %d, write transactions %d", count, noiseXactCommits, noiseWriteTransactions,
				batchMillis, batchXactCommits, batchWriteTransactions, singlesMillis, singlesXactCommits, singlesWriteTransactions);
		LOG.info(report);
		System.out.println("SqlBatchPersistTest.oneCommitPerBatch: " + report);

		assertTrue(batchWriteTransactions >= 1 && batchWriteTransactions - noiseWriteTransactions <= 1 + WRITE_NOISE_TOLERANCE, report);
		assertTrue(singlesWriteTransactions >= count, report);

		long batchXactExcess = batchXactCommits - noiseXactCommits;
		long singlesXactExcess = singlesXactCommits - noiseXactCommits;
		assertTrue(batchXactCommits >= 1 && batchXactExcess <= 1 + XACT_NOISE_TOLERANCE, report);
		assertTrue(singlesXactExcess >= count - XACT_NOISE_TOLERANCE, report);
		assertTrue(batchXactExcess * 4 < singlesXactExcess, report);
	}

	@Test
	public void findByDnsReadsFoundAndSkipsMissing() {
		int count = 1200;
		List<SessionId> sessions = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			sessions.add(buildSession());
		}
		entryManager.persistBatch(sessions.subList(0, 600));
		entryManager.persistBatch(sessions.subList(600, count));

		List<String> dns = new ArrayList<>();
		List<String> missingDns = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			dns.add(sessions.get(i).getDn());
			if (i % 100 == 0) {
				String missingDn = String.format("jansId=%s,%s", UUID.randomUUID(), SESSIONS_BASE_DN);
				missingDns.add(missingDn);
				dns.add(missingDn);
			}
		}
		// Repeated DN is read once
		dns.add(sessions.get(0).getDn());

		Map<String, SessionId> found = entryManager.findByDns(SessionId.class, dns);

		assertEquals(found.size(), count);
		List<String> expectedOrder = new ArrayList<>();
		for (SessionId session : sessions) {
			expectedOrder.add(session.getDn());
		}
		assertEquals(new ArrayList<>(found.keySet()), expectedOrder);
		for (SessionId session : sessions) {
			SessionId foundSession = found.get(session.getDn());
			assertNotNull(foundSession, session.getDn());
			assertEquals(foundSession.getDn(), session.getDn());
			assertEquals(foundSession.getId(), session.getId());
			assertEquals(foundSession.getOutsideSid(), marker);
		}
		for (String missingDn : missingDns) {
			assertFalse(found.containsKey(missingDn));
		}

		assertTrue(entryManager.findByDns(SessionId.class, Collections.emptyList()).isEmpty());
	}

	@Test
	public void findByDnsReadsOneClassPerCall() {
		SessionId session = buildSession();
		SimpleClient client = buildClient();
		entryManager.persistBatch(Arrays.asList(session, client));

		Map<String, SessionId> sessions = entryManager.findByDns(SessionId.class, Arrays.asList(session.getDn(), client.getDn()));
		assertEquals(sessions.keySet(), Collections.singleton(session.getDn()));

		Map<String, SimpleClient> clients = entryManager.findByDns(SimpleClient.class, Arrays.asList(session.getDn(), client.getDn()));
		assertEquals(clients.keySet(), Collections.singleton(client.getDn()));
		assertEquals(clients.get(client.getDn()).getClientName(), client.getClientName());
	}

	@Test
	public void concurrentOverlappingBatchesHaveOneWinner() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			for (int round = 0; round < 5; round++) {
				List<String> ids = new ArrayList<>();
				for (int i = 0; i < 90; i++) {
					ids.add(UUID.randomUUID().toString());
				}

				List<SessionId> first = buildSessions(ids.subList(0, 60));
				List<SessionId> second = buildSessions(ids.subList(30, 90));

				CountDownLatch start = new CountDownLatch(1);
				Future<Throwable> firstResult = executor.submit(persistBatchTask(first, start));
				Future<Throwable> secondResult = executor.submit(persistBatchTask(second, start));
				start.countDown();

				Throwable firstFailure = firstResult.get(60, TimeUnit.SECONDS);
				Throwable secondFailure = secondResult.get(60, TimeUnit.SECONDS);

				assertTrue((firstFailure == null) != (secondFailure == null),
						String.format("Round %d: exactly one batch must win, failures: %s, %s", round, firstFailure, secondFailure));

				Throwable loserFailure = firstFailure == null ? secondFailure : firstFailure;
				assertTrue(loserFailure instanceof EntryPersistenceException, String.valueOf(loserFailure));
				assertTrue(loserFailure.getCause() instanceof DuplicateEntryException, String.valueOf(loserFailure));

				List<SessionId> winner = firstFailure == null ? first : second;
				Set<String> expectedDns = new LinkedHashSet<>();
				for (SessionId session : winner) {
					expectedDns.add(session.getDn());
				}

				List<String> allDns = new ArrayList<>();
				for (String id : ids) {
					allDns.add(sessionDn(id));
				}
				Map<String, SessionId> stored = entryManager.findByDns(SessionId.class, allDns);
				assertEquals(stored.keySet(), expectedDns, "Round " + round);
				for (SessionId session : winner) {
					assertEquals(stored.get(session.getDn()).getJwt(), session.getJwt());
				}
			}
		} finally {
			executor.shutdownNow();
		}
	}

	private Callable<Throwable> persistBatchTask(List<SessionId> batch, CountDownLatch start) {
		return () -> {
			start.await();
			try {
				entryManager.persistBatch(batch);
				return null;
			} catch (Throwable ex) {
				return ex;
			}
		};
	}

	private DuplicateEntryException expectDuplicate(List<?> batch) {
		try {
			entryManager.persistBatch(batch);
			fail("Expected EntryPersistenceException caused by DuplicateEntryException");
		} catch (EntryPersistenceException ex) {
			assertTrue(ex.getCause() instanceof DuplicateEntryException, String.valueOf(ex.getCause()));
			return (DuplicateEntryException) ex.getCause();
		}

		return null;
	}

	private void assertNoneStored(List<SessionId> sessions) {
		List<String> dns = new ArrayList<>();
		for (SessionId session : sessions) {
			dns.add(session.getDn());
		}

		assertTrue(entryManager.findByDns(SessionId.class, dns).isEmpty());
		for (String dn : dns) {
			assertFalse(entryManager.contains(dn, SessionId.class), dn);
		}
	}

	/*
	 * [xact_commit, next transaction id] of the current database after statistics settled
	 */
	private long[] readCommitCounters(SqlEntryManager sqlEntryManager) throws Exception {
		Thread.sleep(STATS_SETTLE_MILLIS);

		try (Connection connection = sqlEntryManager.getOperationService().getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT xact_commit, txid_snapshot_xmax(txid_current_snapshot()) "
						+ "FROM pg_stat_database WHERE datname = current_database()")) {
			assertTrue(resultSet.next());
			return new long[] { resultSet.getLong(1), resultSet.getLong(2) };
		}
	}

	private List<SessionId> buildSessions(List<String> ids) {
		List<SessionId> result = new ArrayList<>();
		for (String id : ids) {
			SessionId session = buildSession();
			session.setId(id);
			session.setDn(sessionDn(id));
			session.setJwt(String.format("{\"batch\":\"%s\"}", UUID.randomUUID()));
			result.add(session);
		}

		return result;
	}

	private SessionId buildSession() {
		Date now = new Date((System.currentTimeMillis() / 1000) * 1000);

		SessionId session = new SessionId();
		session.setId(UUID.randomUUID().toString());
		session.setDn(sessionDn(session.getId()));
		session.setOutsideSid(marker);
		session.setCreationDate(now);
		session.setLastUsedAt(now);
		session.setAuthenticationTime(now);
		session.setExpirationDate(new Date(now.getTime() + 120000L));
		session.setUserDn("inum=batch-user,ou=people,o=jans");
		session.setState(SessionIdState.AUTHENTICATED);
		session.setPermissionGranted(Boolean.TRUE);
		session.setJwt("{}");
		session.setIsJwt(true);

		Map<String, String> sessionAttributes = new HashMap<>();
		sessionAttributes.put("marker", marker);
		session.setSessionAttributes(sessionAttributes);

		return session;
	}

	private String sessionDn(String id) {
		return String.format("jansId=%s,%s", id, SESSIONS_BASE_DN);
	}

	private SimpleClient buildClient() {
		String inum = getRandomInum() + "-" + UUID.randomUUID().toString().substring(0, 8);

		SimpleClient client = new SimpleClient();
		client.setDn(String.format("inum=%s,%s", inum, CLIENTS_BASE_DN));
		client.setClientName(marker + "_" + inum);
		client.setDefaultAcrValues(new String[] { marker });
		createdClientDns.add(client.getDn());

		return client;
	}

	private void copySessionValues(SessionId source, SessionId target) {
		target.setCreationDate(source.getCreationDate());
		target.setLastUsedAt(source.getLastUsedAt());
		target.setAuthenticationTime(source.getAuthenticationTime());
		target.setExpirationDate(source.getExpirationDate());
		target.setUserDn(source.getUserDn());
		target.setState(source.getState());
		target.setPermissionGranted(source.getPermissionGranted());
		target.setJwt(source.getJwt());
		target.setIsJwt(source.getIsJwt());
		target.setSessionAttributes(new HashMap<>(source.getSessionAttributes()));
	}

	private void assertSessionValuesEqual(SessionId actual, SessionId expected) {
		assertEquals(actual.getOutsideSid(), expected.getOutsideSid());
		assertEquals(actual.getCreationDate(), expected.getCreationDate());
		assertEquals(actual.getLastUsedAt(), expected.getLastUsedAt());
		assertEquals(actual.getAuthenticationTime(), expected.getAuthenticationTime());
		assertEquals(actual.getExpirationDate(), expected.getExpirationDate());
		assertEquals(actual.getUserDn(), expected.getUserDn());
		assertEquals(actual.getState(), expected.getState());
		assertEquals(actual.getPermissionGranted(), expected.getPermissionGranted());
		assertEquals(actual.getJwt(), expected.getJwt());
		assertEquals(actual.getIsJwt(), expected.getIsJwt());
		assertEquals(actual.getSessionAttributes(), expected.getSessionAttributes());
		assertEquals(actual.isDeletable(), expected.isDeletable());
		assertNull(actual.getSessionState());
	}

	private void removeQuietly(Set<String> dns, Class<?> entryClass) {
		synchronized (dns) {
			for (String dn : dns) {
				try {
					if (entryManager.contains(dn, entryClass)) {
						entryManager.remove(dn, entryClass);
					}
				} catch (Exception ex) {
					LOG.warn("Failed to remove test entry '{}'", dn, ex);
				}
			}
		}
	}

}
