/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks.StaticPolicyDownloadService;

class PolicyMismatchCheckTest {

	private static final String STORE = "store-1";

	private static final String HASH = "sha256:" + "a".repeat(64);

	private static final String NEXT_HASH = "sha256:" + "b".repeat(64);

	private static final long T0 = 1_781_000_000L;

	private static final int GRACE_SECONDS = 300;

	private StaticPolicyDownloadService served;

	private AppConfiguration appConfiguration;

	private PolicyMismatchCheck check;

	@BeforeEach
	void setUp() {
		served = new StaticPolicyDownloadService().archive(STORE, "1.0.0", HASH).json("https://policy.example.org/json");
		appConfiguration = new AppConfiguration();
		appConfiguration.getTraceConfiguration().setPolicyMismatchGraceSeconds(GRACE_SECONDS);
		check = PolicyMismatchChecks.serving(served, appConfiguration);
		at(T0);
	}

	private void at(long epochSeconds) {
		PolicyMismatchChecks.setClock(check, Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC));
	}

	private void rollOutNextRelease(long epochSeconds) {
		at(T0);
		check.check(new PolicyRef(STORE, "1.0.0", HASH), T0);
		served.archive(STORE, "2.0.0", NEXT_HASH);
		at(epochSeconds);
		check.check(new PolicyRef(STORE, "2.0.0", NEXT_HASH), epochSeconds);
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
	void testCheck_LateRecordUnderRetiredReleaseWithinGrace_NoMismatch() {
		rollOutNextRelease(T0 + 60);

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + 60 + GRACE_SECONDS));
	}

	@Test
	void testCheck_RecordSignedAfterGraceUnderRetiredRelease_Mismatch() {
		rollOutNextRelease(T0 + 60);

		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + 60 + GRACE_SECONDS + 1));
	}

	@Test
	void testCheck_NewReleaseAfterRollout_NoMismatch() {
		rollOutNextRelease(T0 + 60);

		assertFalse(check.check(new PolicyRef(STORE, "2.0.0", NEXT_HASH), T0 + 120));
	}

	@Test
	void testCheck_ReleaseRetiredBeforeRestart_Mismatch() {
		served.archive(STORE, "2.0.0", NEXT_HASH);

		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0));
	}

	private void publish(Map<String, LoadedPolicySource> previous, Map<String, LoadedPolicySource> current,
			long epochSeconds) {
		check.onPolicySnapshot(new PolicySnapshotEvent(previous, current, epochSeconds * 1000L));
	}

	@Test
	void testCheck_RolloutWithoutTraffic_RecordWithinGraceNotFlagged() {
		Map<String, LoadedPolicySource> first = Map.of(STORE, PolicyMismatchChecks.archiveSource(STORE, "1.0.0", HASH));
		Map<String, LoadedPolicySource> second = Map.of(STORE,
				PolicyMismatchChecks.archiveSource(STORE, "2.0.0", NEXT_HASH));
		publish(Map.of(), first, T0);
		publish(first, second, T0 + 60);
		served.archive(STORE, "2.0.0", NEXT_HASH);
		at(T0 + 3600);

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + 60 + GRACE_SECONDS));
	}

	@Test
	void testCheck_RolloutWithoutTraffic_RecordAfterGraceFlagged() {
		Map<String, LoadedPolicySource> first = Map.of(STORE, PolicyMismatchChecks.archiveSource(STORE, "1.0.0", HASH));
		Map<String, LoadedPolicySource> second = Map.of(STORE,
				PolicyMismatchChecks.archiveSource(STORE, "2.0.0", NEXT_HASH));
		publish(Map.of(), first, T0);
		publish(first, second, T0 + 60);
		served.archive(STORE, "2.0.0", NEXT_HASH);
		at(T0 + 3600);

		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", HASH), T0 + 60 + GRACE_SECONDS + 1));
	}

	@Test
	void testCheck_LoadedPoliciesFails_NoMismatch() {
		PolicyDownloadService failing = new PolicyDownloadService() {
			@Override
			public Map<String, LoadedPolicySource> getLoadedPolicies() {
				throw new IllegalStateException("reload in progress");
			}
		};
		PolicyMismatchCheck failingCheck = PolicyMismatchChecks.serving(failing, appConfiguration);

		assertFalse(failingCheck.check(new PolicyRef(STORE, "1.0.0", NEXT_HASH), T0));
	}

}
