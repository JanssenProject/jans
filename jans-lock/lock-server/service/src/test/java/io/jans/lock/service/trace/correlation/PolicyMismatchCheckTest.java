/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.core.cedarling.service.policy.PolicySnapshotEvent;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks.StaticPolicyDownloadService;

class PolicyMismatchCheckTest {

	private static final String STORE = "store-1";

	private static final String HASH = "sha256:" + "a".repeat(64);

	private static final String NEXT_HASH = "sha256:" + "b".repeat(64);

	private static final long T0 = 1_781_000_000_000L;

	private static final long GRACE_MS = 300_000L;

	private static final Map<String, LoadedPolicySource> FIRST = Map.of(STORE,
			PolicyMismatchChecks.archiveSource(STORE, "1.0.0", HASH));

	private static final Map<String, LoadedPolicySource> SECOND = Map.of(STORE,
			PolicyMismatchChecks.archiveSource(STORE, "2.0.0", NEXT_HASH));

	private StaticPolicyDownloadService served;

	private AppConfiguration appConfiguration;

	private InMemoryTraceStore store;

	private PolicyMismatchCheck check;

	@BeforeEach
	void setUp() {
		served = new StaticPolicyDownloadService().archive(STORE, "1.0.0", HASH).json("https://policy.example.org/json");
		appConfiguration = new AppConfiguration();
		appConfiguration.getTraceConfiguration().setPolicyMismatchGraceSeconds((int) (GRACE_MS / 1000));
		store = new InMemoryTraceStore();
		check = node(served);
	}

	private PolicyMismatchCheck node(PolicyDownloadService nodeServed) {
		PolicyMismatchCheck node = PolicyMismatchChecks.serving(nodeServed, appConfiguration, store);
		PolicyMismatchChecks.setClock(node, Clock.fixed(Instant.ofEpochMilli(T0), ZoneOffset.UTC));
		return node;
	}

	private static void publish(PolicyMismatchCheck node, Map<String, LoadedPolicySource> previous,
			Map<String, LoadedPolicySource> current, long atMs) {
		node.onPolicySnapshot(new PolicySnapshotEvent(previous, current, atMs));
	}

	private void rollOutSecondRelease() {
		publish(check, Map.of(), FIRST, T0);
		served.archive(STORE, "2.0.0", NEXT_HASH);
		publish(check, FIRST, SECOND, T0 + 60_000L);
	}

	@Test
	void testCheck_SameRelease_NoMismatch() {
		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0));
	}

	@Test
	void testCheck_UnknownBundleHash_Mismatch() {
		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", NEXT_HASH), T0));
	}

	@Test
	void testCheck_DifferentVersionSameHash_Mismatch() {
		assertTrue(check.check(new PolicyRef(STORE, "2.0.0", HASH), T0));
	}

	@Test
	void testCheck_StoreNotServed_NoMismatch() {
		assertFalse(check.check(new PolicyRef("store-elsewhere", "1.0.0", NEXT_HASH), T0));
	}

	@Test
	void testCheck_JsonSourceIsNotComparable_NoMismatch() {
		assertFalse(check.check(new PolicyRef("https://policy.example.org/json", "1.0.0", NEXT_HASH), T0));
	}

	@Test
	void testCheck_NoPolicy_NoMismatch() {
		assertFalse(check.check(null, T0));
	}

	@Test
	void testCheck_NothingLoadedYet_NoMismatch() {
		served.remove(STORE);

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", NEXT_HASH), T0));
	}

	@Test
	void testCheck_RetiredReleaseWithinGrace_NoMismatch() {
		rollOutSecondRelease();

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + GRACE_MS));
	}

	@Test
	void testCheck_RetiredReleaseAfterGrace_Mismatch() {
		rollOutSecondRelease();

		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + GRACE_MS + 1));
	}

	@Test
	void testCheck_ReleaseServedByAnotherNode_NoMismatch() {
		PolicyMismatchCheck nodeA = node(new StaticPolicyDownloadService().archive(STORE, "2.0.0", NEXT_HASH));
		publish(nodeA, Map.of(), SECOND, T0);

		assertFalse(check.check(new PolicyRef(STORE, "2.0.0", NEXT_HASH), T0 + 10_000L));
	}

	@Test
	void testCheck_UnknownReleaseNotCached_SeenOnceAnotherNodeRecordsIt() {
		PolicyRef second = new PolicyRef(STORE, "2.0.0", NEXT_HASH);
		assertTrue(check.check(second, T0));

		PolicyMismatchCheck nodeA = node(new StaticPolicyDownloadService().archive(STORE, "2.0.0", NEXT_HASH));
		publish(nodeA, Map.of(), SECOND, T0);

		assertFalse(check.check(second, T0 + 1_000L));
	}

	@Test
	void testCheck_HistorySurvivesRestart_NoMismatch() {
		rollOutSecondRelease();
		PolicyMismatchCheck restarted = node(served);

		assertFalse(restarted.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + 100_000L));
	}

	@Test
	void testOnPolicySnapshot_LastServedUpdateIsThrottled() {
		publish(check, Map.of(), FIRST, T0);
		publish(check, FIRST, FIRST, T0 + 30_000L);
		publish(check, FIRST, FIRST, T0 + 70_000L);

		assertEquals(2, store.getPolicyReleaseWrites());
		assertEquals(T0 + 70_000L, store.findPolicyRelease(STORE, HASH).orElseThrow().getLastServedAtMs());
		assertEquals(T0, store.findPolicyRelease(STORE, HASH).orElseThrow().getFirstServedAtMs());
	}

	@Test
	void testOnPolicySnapshot_LaggingNodeCannotMoveLastServedBack() {
		publish(check, Map.of(), FIRST, T0 + 100_000L);
		PolicyMismatchCheck lagging = node(new StaticPolicyDownloadService().archive(STORE, "1.0.0", HASH));
		publish(lagging, Map.of(), FIRST, T0);

		assertEquals(T0 + 100_000L, store.findPolicyRelease(STORE, HASH).orElseThrow().getLastServedAtMs());
		assertEquals(T0, store.findPolicyRelease(STORE, HASH).orElseThrow().getFirstServedAtMs());
	}

	@Test
	void testCheck_ZeroGraceStillCoversTwoReloadIntervals() {
		appConfiguration.getTraceConfiguration().setPolicyMismatchGraceSeconds(0);
		rollOutSecondRelease();
		long reloadWindowMs = 2_000L * PolicyDownloadService.RELOAD_INTERVAL_SECONDS;

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + reloadWindowMs));
		assertTrue(node(served).check(new PolicyRef(STORE, "1.0.0", HASH), T0 + reloadWindowMs + 1));
	}

	@Test
	void testCache_EvictsLeastRecentlyUsed() {
		Map<String, LoadedPolicySource> many = new java.util.LinkedHashMap<>();
		for (int i = 0; i < PolicyMismatchCheck.CACHE_MAX_ENTRIES; i++) {
			many.put("store-" + i, PolicyMismatchChecks.archiveSource("store-" + i, "1.0.0", hashOf(i)));
		}
		publish(check, Map.of(), many, T0);
		served.archive("store-0", "2.0.0", NEXT_HASH);
		check.check(new PolicyRef("store-0", "1.0.0", hashOf(0)), T0);
		publish(check, many, Map.of("store-new", PolicyMismatchChecks.archiveSource("store-new", "1.0.0", HASH)), T0);

		assertEquals(PolicyMismatchCheck.CACHE_MAX_ENTRIES, check.cachedReleaseCount());
		assertTrue(check.isCached("store-0", hashOf(0)));
		assertFalse(check.isCached("store-1", hashOf(1)));
		assertTrue(check.isCached("store-new", HASH));
	}

	private static String hashOf(int i) {
		return "sha256:" + String.format("%064x", i);
	}

	@Test
	void testCheck_LoadedPoliciesFails_NoMismatch() {
		PolicyDownloadService failing = new PolicyDownloadService() {
			@Override
			public Map<String, LoadedPolicySource> getLoadedPolicies() {
				throw new IllegalStateException("reload in progress");
			}
		};

		assertFalse(node(failing).check(new PolicyRef(STORE, "1.0.0", NEXT_HASH), T0));
	}

	@Test
	void testCheck_StoreFails_NoMismatch() {
		TraceStore failingStore = mock(TraceStore.class);
		when(failingStore.findPolicyRelease(anyString(), anyString()))
				.thenThrow(new TraceStorageException("read_failed", "database down"));
		PolicyMismatchCheck node = PolicyMismatchChecks.serving(served, appConfiguration, failingStore);

		assertFalse(node.check(new PolicyRef(STORE, "1.0.0", NEXT_HASH), T0));
		publish(node, Map.of(), FIRST, T0);
	}

}
