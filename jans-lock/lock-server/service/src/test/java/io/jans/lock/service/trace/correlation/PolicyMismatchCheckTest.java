/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.PolicyRef;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks.StaticPolicyDownloadService;

class PolicyMismatchCheckTest {

	private static final String STORE = "store-1";

	private static final String HASH = "sha256:" + "a".repeat(64);

	private static final String OTHER_HASH = "sha256:" + "b".repeat(64);

	private AppConfiguration appConfiguration;

	private PolicyMismatchCheck check;

	@BeforeEach
	void setUp() {
		appConfiguration = new AppConfiguration();
		check = PolicyMismatchChecks.serving(appConfiguration,
				new StaticPolicyDownloadService().archive(STORE, "1.0.0", HASH).json("https://policy.example.org/json"));
	}

	@Test
	void testCheck_SameRelease_NoMismatch() {
		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH)));
	}

	@Test
	void testCheck_DifferentBundleHash_Mismatch() {
		assertTrue(check.check(new PolicyRef(STORE, "1.0.0", OTHER_HASH)));
	}

	@Test
	void testCheck_DifferentVersionSameHash_Mismatch() {
		assertTrue(check.check(new PolicyRef(STORE, "2.0.0", HASH)));
	}

	@Test
	void testCheck_StoreNotServed_NoMismatch() {
		assertFalse(check.check(new PolicyRef("store-elsewhere", "1.0.0", OTHER_HASH)));
	}

	@Test
	void testCheck_JsonSourceIsNotComparable_NoMismatch() {
		assertFalse(check.check(new PolicyRef("https://policy.example.org/json", "1.0.0", OTHER_HASH)));
	}

	@Test
	void testCheck_NoPolicy_NoMismatch() {
		assertFalse(check.check(null));
	}

	@Test
	void testCheck_RejectEnabled_MismatchThrowsInvalidAssertion() {
		appConfiguration.getTraceConfiguration().setRejectPolicyMismatch(true);

		PolicyRef policy = new PolicyRef(STORE, "1.0.0", OTHER_HASH);
		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> check.check(policy));

		assertEquals(TraceErrorResponseType.INVALID_ASSERTION, ex.getErrorId());
		assertEquals(PolicyMismatchCheck.REASON_POLICY_MISMATCH, ex.getReason());
	}

	@Test
	void testCheck_RejectEnabled_MatchingOrUnservedReleaseAccepted() {
		appConfiguration.getTraceConfiguration().setRejectPolicyMismatch(true);

		assertFalse(check.check(new PolicyRef(STORE, "1.0.0", HASH)));
		assertFalse(check.check(new PolicyRef("store-elsewhere", "1.0.0", OTHER_HASH)));
	}

}
