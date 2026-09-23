/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import org.junit.jupiter.api.Test;

import io.jans.fido2.model.audit.LockAuditEvent;
import io.jans.orm.model.fido2.Fido2RegistrationData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the mapping from an authentication outcome onto the Lock Server audit-event wire shape
 * ({@code AssertionService#buildAuthenticationAuditEvent}), independent of {@code verify()}'s full
 * dependency graph (authentication persistence, assertion verification, external scripts, ...), none
 * of which this mapping touches.
 * <p>
 * {@code verify()}'s own decisions about which {@code origin} and {@code failure} values to pass —
 * including the "already committed as authenticated" case where a post-persistence exception must
 * still map to ALLOW, not DENY — are not covered here, since they live in {@code verify()} itself,
 * which has no full unit test in this codebase for its happy path (see
 * {@code AssertionServiceTest#verify_ifCeremonyAlreadyTerminal_collectsAnAllowLockAuditEventNotDeny}
 * for that specific branch). This file only proves the mapper does the right thing with whatever it
 * is given.
 */
class AssertionServiceLockAuditTest {

	private final AssertionService assertionService = new AssertionService();

	@Test
	void buildAuthenticationAuditEvent_onSuccess_carriesRegistrationDataAndAllows() {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setRpId("my.jans.server");
		registrationData.setPublicKeyId("cred-123");

		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", registrationData,
				"https://my.jans.server", "platform", null);

		assertEquals("fido2", event.getService());
		assertEquals("fido2_authentication", event.getEventType());
		assertEquals("authenticate", event.getAction());
		assertEquals("alice", event.getPrincipalId());
		assertEquals("info", event.getSeverityLevel());
		assertEquals("ALLOW", event.getDecisionResult());
		assertNotNull(event.getEventTime());

		assertEquals("my.jans.server", event.getContextInformation().get("rpId"));
		assertEquals("cred-123", event.getContextInformation().get("credentialId"));
		assertEquals("https://my.jans.server", event.getContextInformation().get("origin"));
		assertEquals("platform", event.getContextInformation().get("authenticatorAttachment"));
	}

	/**
	 * A credential can be registered at one permitted origin of an RP and used from a different
	 * permitted origin later, so the recorded origin must come from the {@code origin} parameter
	 * (the current ceremony), never from {@code registrationData} (where it was originally
	 * registered) even when both are supplied.
	 */
	@Test
	void buildAuthenticationAuditEvent_originDiffersFromRegistrationOrigin_recordsTheCeremonyOrigin() {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setRpId("my.jans.server");
		registrationData.setOrigin("https://original.my.jans.server");

		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", registrationData,
				"https://second.my.jans.server", "platform", null);

		assertEquals("https://second.my.jans.server", event.getContextInformation().get("origin"));
	}

	/**
	 * A failure can occur before the registration entry is even looked up, so this must not throw or
	 * silently swallow the event just because {@code registrationData} is {@code null}.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureBeforeRegistrationDataExists_stillProducesADenyEvent() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent(null, null, null, null,
				new IllegalStateException("boom"));

		assertEquals("warning", event.getSeverityLevel());
		assertEquals("DENY", event.getDecisionResult());
		assertNull(event.getPrincipalId());
		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
	}

	/**
	 * A failure raised after {@code registrationData} is loaded (assertion verification, signature
	 * checks, etc. all happen after the lookup) must not discard the rpId/origin context just because
	 * the outcome is a failure — {@code verify()} passes whatever it actually has at the point of
	 * failure, not unconditionally {@code null}.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureAfterRegistrationDataLoaded_preservesRpIdAndOrigin() {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setRpId("my.jans.server");

		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", registrationData,
				"https://my.jans.server", "platform", new IllegalStateException("boom"));

		assertEquals("DENY", event.getDecisionResult());
		assertEquals("my.jans.server", event.getContextInformation().get("rpId"));
		assertEquals("https://my.jans.server", event.getContextInformation().get("origin"));
	}

	/**
	 * The audit trail must not duplicate identifying detail already carried in exception messages
	 * elsewhere in this class (challenge, username) — only the exception's class name is recorded.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailure_neverIncludesTheExceptionMessage() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", null, null, null,
				new IllegalStateException("Challenge in clientData does not match 'abc123'"));

		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
		event.getContextInformation().values().forEach(value -> {
			if (value.contains("abc123")) {
				throw new AssertionError("audit event context leaked the exception message: " + value);
			}
		});
	}
}
