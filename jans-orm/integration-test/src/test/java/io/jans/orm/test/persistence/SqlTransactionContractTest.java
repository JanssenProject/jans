/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.test.persistence;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.test.BaseOrmTest;
import io.jans.orm.test.model.VersionedTestEntry;

/**
 * Live SQL contract of {@code executeInTransaction} / {@code findForUpdate}. Skipped when the active
 * {@code -Dcfg} profile is not SQL. Uses the {@code jansTestVersioned} table of
 * {@link VersionedUpdateContractTest}.
 */
public class SqlTransactionContractTest extends BaseOrmTest {

	private static final String BASE_DN = "ou=version_test,o=jans";

	@BeforeClass
	public void requireSql() {
		requirePersistenceType(SQL);
	}

	@Test
	public void committedTransactionStoresAllWrites() {
		VersionedTestEntry first = newEntry("commit-1");
		VersionedTestEntry second = newEntry("commit-2");

		entryManager.executeInTransaction(BASE_DN, () -> {
			entryManager.persist(first);
			entryManager.persist(second);
			return null;
		});

		assertTrue(entryManager.contains(first.getDn(), VersionedTestEntry.class));
		assertTrue(entryManager.contains(second.getDn(), VersionedTestEntry.class));
		cleanUp(first, second);
	}

	@Test
	public void failingTransactionRollsBackEveryWrite() {
		VersionedTestEntry first = newEntry("rollback-1");
		VersionedTestEntry second = newEntry("rollback-2");

		expectThrows(IllegalStateException.class, () -> entryManager.executeInTransaction(BASE_DN, () -> {
			entryManager.persist(first);
			entryManager.persist(second);
			throw new IllegalStateException("abort");
		}));

		assertFalse(entryManager.contains(first.getDn(), VersionedTestEntry.class));
		assertFalse(entryManager.contains(second.getDn(), VersionedTestEntry.class));
	}

	@Test
	public void versionedUpdateInsideRolledBackTransactionIsUndone() {
		VersionedTestEntry entry = newEntry("cas");
		entryManager.persist(entry);

		expectThrows(IllegalStateException.class, () -> entryManager.executeInTransaction(BASE_DN, () -> {
			VersionedTestEntry locked = entryManager.findForUpdate(VersionedTestEntry.class, entry.getDn());
			locked.setData("changed");
			entryManager.updateWithVersion(locked);
			throw new IllegalStateException("abort");
		}));

		VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
		assertEquals(found.getData(), "cas");
		assertEquals(found.getVersion(), entry.getVersion());
		cleanUp(entry);
	}

	@Test
	public void duplicatePersistInsideTransactionReportsDuplicateAndRollsBack() {
		VersionedTestEntry existing = newEntry("dup");
		entryManager.persist(existing);
		VersionedTestEntry other = newEntry("dup-other");

		EntryPersistenceException ex = expectThrows(EntryPersistenceException.class,
				() -> entryManager.executeInTransaction(BASE_DN, () -> {
					entryManager.persist(other);
					VersionedTestEntry duplicate = newEntry("dup");
					duplicate.setDn(existing.getDn());
					entryManager.persist(duplicate);
					return null;
				}));

		assertTrue(hasCause(ex, DuplicateEntryException.class));
		assertFalse(entryManager.contains(other.getDn(), VersionedTestEntry.class));
		cleanUp(existing);
	}

	@Test
	public void findForUpdateMissingEntryReturnsNull() {
		Object result = entryManager.executeInTransaction(BASE_DN,
				() -> entryManager.findForUpdate(VersionedTestEntry.class, String.format("uid=%s,%s", UUID.randomUUID(), BASE_DN)));

		assertNull(result);
	}

	@Test
	public void findForUpdateOutsideTransactionThrows() {
		expectThrows(IllegalStateException.class, () -> entryManager.findForUpdate(VersionedTestEntry.class, "uid=x," + BASE_DN));
	}

	@Test
	public void caughtDuplicateInsideTransactionStillRollsBack() {
		VersionedTestEntry existing = newEntry("caught-dup");
		entryManager.persist(existing);
		VersionedTestEntry other = newEntry("caught-dup-other");

		expectThrows(EntryPersistenceException.class, () -> entryManager.executeInTransaction(BASE_DN, () -> {
			entryManager.persist(other);
			VersionedTestEntry duplicate = newEntry("caught-dup");
			duplicate.setDn(existing.getDn());
			try {
				entryManager.persist(duplicate);
			} catch (EntryPersistenceException expected) {
				return "done";
			}
			return "unexpected";
		}));

		assertFalse(entryManager.contains(other.getDn(), VersionedTestEntry.class));
		cleanUp(existing);
	}

	@Test
	public void findForUpdateBlocksSecondTransactionUntilFirstCommits() throws Exception {
		assertLockHeldUntilEnd(false);
	}

	@Test
	public void findForUpdateBlocksSecondTransactionUntilFirstRollsBack() throws Exception {
		assertLockHeldUntilEnd(true);
	}

	private void assertLockHeldUntilEnd(boolean rollback) throws Exception {
		VersionedTestEntry entry = newEntry("lock");
		entryManager.persist(entry);

		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch waiterReady = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> holder = executor.submit(() -> entryManager.executeInTransaction(BASE_DN, () -> {
				VersionedTestEntry row = entryManager.findForUpdate(VersionedTestEntry.class, entry.getDn());
				locked.countDown();
				assertTrue(release.await(30, TimeUnit.SECONDS));
				row.setData("holder");
				entryManager.updateWithVersion(row);
				if (rollback) {
					throw new IllegalStateException("abort");
				}
				return null;
			}));

			assertTrue(locked.await(10, TimeUnit.SECONDS));
			Future<String> waiter = executor.submit(() -> entryManager.executeInTransaction(BASE_DN, () -> {
				waiterReady.countDown();
				return entryManager.findForUpdate(VersionedTestEntry.class, entry.getDn()).getData();
			}));

			assertTrue(waiterReady.await(10, TimeUnit.SECONDS));
			expectThrows(TimeoutException.class, () -> waiter.get(1, TimeUnit.SECONDS));

			release.countDown();
			if (rollback) {
				ExecutionException failure = expectThrows(ExecutionException.class, () -> holder.get(10, TimeUnit.SECONDS));
				assertTrue(hasCause(failure, IllegalStateException.class));
			} else {
				holder.get(10, TimeUnit.SECONDS);
			}
			assertEquals(waiter.get(10, TimeUnit.SECONDS), rollback ? "lock" : "holder");
		} finally {
			release.countDown();
			executor.shutdownNow();
		}

		VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
		assertNotNull(found);
		cleanUp(entry);
	}

	private VersionedTestEntry newEntry(String data) {
		VersionedTestEntry entry = new VersionedTestEntry();
		String uid = UUID.randomUUID().toString();
		entry.setDn(String.format("uid=%s,%s", uid, BASE_DN));
		entry.setUid(uid);
		entry.setData(data);
		return entry;
	}

	private void cleanUp(VersionedTestEntry... entries) {
		for (VersionedTestEntry entry : entries) {
			entryManager.remove(entry.getDn(), VersionedTestEntry.class);
		}
	}

	private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeClass) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (causeClass.isInstance(current)) {
				return true;
			}
		}
		return false;
	}

}
