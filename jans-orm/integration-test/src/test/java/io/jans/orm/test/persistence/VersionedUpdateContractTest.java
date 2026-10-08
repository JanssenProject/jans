/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.persistence;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.VersionMismatchException;
import io.jans.orm.test.BaseOrmTest;
import io.jans.orm.test.model.VersionedTestEntry;

/**
 * Shared, backend-agnostic CAS (`@Version` / `updateWithVersion`) contract test suite. Asserts through:
 * sequential CAS success, stale-version rejection,
 * concurrent-writers-exactly-one-winner, `merge()` still bumping the version, and the pre-read
 * not-found behavior staying distinct from a version conflict.
 *
 * <p>One concrete subclass per landed backend (mirrors
 * {@code io.jans.lock.service.trace.store.TraceStoreContractTest}'s abstract-base-class pattern,
 * adapted to TestNG): each subclass only fixes {@link #requiredPersistenceType()} and is skipped
 * via {@link BaseOrmTest#requirePersistenceType(String...)} when the active {@code -Dcfg} profile
 * doesn't match it, so running the whole suite against a single live profile still exercises
 * exactly the backend that profile provides.</p>
 *
 * <p>Requires {@code jansVersion} attribute/column already provisioned for the
 * {@code jansTestVersioned} object class wherever the active profile points (SQL table column or
 * LDAP schema; see {@link VersionedTestEntry}) -- this test's own code is not what provisions it.</p>
 *
 * @author Yuriy Movchan
 */
public abstract class VersionedUpdateContractTest extends BaseOrmTest {

	private static final String VERSION_TEST_BASE_DN = "ou=version_test,o=jans";

	protected abstract String requiredPersistenceType();

	@BeforeClass
	public void requireOwnPersistenceType() {
		requirePersistenceType(requiredPersistenceType());
	}

	@Test
	public void sequentialUpdatesAdvanceVersionByOnePerCall() {
		VersionedTestEntry entry = persistNewEntry("seq");
		long startVersion = entry.getVersion();

		entry.setData("seq-1");
		entryManager.updateWithVersion(entry);

		entry.setData("seq-2");
		entryManager.updateWithVersion(entry);

		VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
		assertEquals(found.getVersion().longValue(), startVersion + 2);
		assertEquals(found.getData(), "seq-2");

		entryManager.remove(entry.getDn(), VersionedTestEntry.class);
	}

	@Test
	public void staleCopyUpdateThrowsVersionMismatchAndLeavesRowUnchanged() {
		VersionedTestEntry entry = persistNewEntry("stale");

		// Simulate a second reader holding the pre-update snapshot
		VersionedTestEntry staleCopy = entryManager.find(VersionedTestEntry.class, entry.getDn());

		entry.setData("winner");
		entryManager.updateWithVersion(entry);

		staleCopy.setData("loser");
		try {
			entryManager.updateWithVersion(staleCopy);
			fail("Expected VersionMismatchException for a stale version");
		} catch (VersionMismatchException ex) {
			assertEquals(ex.getDn(), entry.getDn());
		}

		VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
		assertEquals(found.getData(), "winner");

		entryManager.remove(entry.getDn(), VersionedTestEntry.class);
	}

	/*
	 * "not found" only means VersionMismatchException when the row is
	 * deleted in the race window between updateWithVersion's own pre-read and its write.
	 * updateWithVersion pre-reads the entry the same way merge() does, so a DN that was
	 * already gone before the call began surfaces merge()'s normal not-found behavior instead
	 * (EntryPersistenceException for SQL) -- same as calling merge() on a nonexistent entry today.
	 */
	@Test
	public void updateAgainstNeverExistedDnThrowsSameNotFoundExceptionAsMerge() {
		VersionedTestEntry entry = new VersionedTestEntry();
		entry.setDn(String.format("uid=%s,%s", UUID.randomUUID(), VERSION_TEST_BASE_DN));
		entry.setUid("does-not-exist");
		entry.setData("n/a");
		entry.setVersion(0L);

		try {
			entryManager.updateWithVersion(entry);
			fail("Expected EntryPersistenceException for a DN that never existed");
		} catch (EntryPersistenceException ex) {
			// Expected -- same not-found behavior as merge() against a missing entry
		}
	}

	@Test
	public void concurrentWritersWithSameStartingVersionYieldExactlyOneWinner() throws InterruptedException {
		VersionedTestEntry entry = persistNewEntry("race");

		VersionedTestEntry copyA = entryManager.find(VersionedTestEntry.class, entry.getDn());
		VersionedTestEntry copyB = entryManager.find(VersionedTestEntry.class, entry.getDn());

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger conflictCount = new AtomicInteger();
		AtomicReference<Throwable> workerFailure = new AtomicReference<>();

		CountDownLatch startLatch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		Runnable raceA = () -> runRacingUpdate(copyA, "writer-a", startLatch, successCount, conflictCount, workerFailure);
		Runnable raceB = () -> runRacingUpdate(copyB, "writer-b", startLatch, successCount, conflictCount, workerFailure);


		executorService.execute(raceA);
		executorService.execute(raceB);
		startLatch.countDown();

		executorService.shutdown();
		executorService.awaitTermination(60, TimeUnit.SECONDS);
		boolean terminated = executorService.awaitTermination(60, TimeUnit.SECONDS);
		Throwable failure = workerFailure.get();
		if (failure != null) {
			throw new AssertionError("Unexpected exception in race worker", failure);
		}
		assertTrue(terminated, "Race workers did not terminate");

		assertEquals(successCount.get(), 1, "Expected exactly one winner");
		assertEquals(conflictCount.get(), 1, "Expected exactly one VersionMismatchException");

		entryManager.remove(entry.getDn(), VersionedTestEntry.class);
	}

	@Test
	public void mergeOnVersionedEntityAlwaysSucceedsAndBumpsVersion() {
		VersionedTestEntry entry = persistNewEntry("merge");
		long startVersion = entry.getVersion();

		entry.setData("merged");
		entryManager.merge(entry);

		VersionedTestEntry found = entryManager.find(VersionedTestEntry.class, entry.getDn());
		assertEquals(found.getData(), "merged");
		assertNotEquals(found.getVersion().longValue(), startVersion);
		assertEquals(found.getVersion().longValue(), startVersion + 1);

		entryManager.remove(entry.getDn(), VersionedTestEntry.class);
	}

	private void runRacingUpdate(VersionedTestEntry entry, String newData, CountDownLatch startLatch,
			AtomicInteger successCount, AtomicInteger conflictCount, AtomicReference<Throwable> workerFailure) {
		try {
			startLatch.await();
			entry.setData(newData);
			entryManager.updateWithVersion(entry);
			successCount.incrementAndGet();
		} catch (VersionMismatchException ex) {
			conflictCount.incrementAndGet();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (Throwable ex) {
			workerFailure.compareAndSet(null, ex);
		}
	}

	private VersionedTestEntry persistNewEntry(String label) {
		VersionedTestEntry entry = new VersionedTestEntry();
		String uid = label + "-" + UUID.randomUUID();
		entry.setDn(String.format("uid=%s,%s", uid, VERSION_TEST_BASE_DN));
		entry.setUid(uid);
		entry.setData("initial");

		entryManager.persist(entry);

		return entryManager.find(VersionedTestEntry.class, entry.getDn());
	}

}
