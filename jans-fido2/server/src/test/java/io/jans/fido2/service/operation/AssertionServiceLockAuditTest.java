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
 * dependency graph (challenge/entry lookups, assertion verification, external scripts, ...), none of
 * which this mapping touches. Mirrors {@code AttestationServiceLockAuditTest} for the registration side.
 */
class AssertionServiceLockAuditTest {

	private final AssertionService assertionService = new AssertionService();

	@Test
	void buildAuthenticationAuditEvent_onSuccess_carriesRegistrationDataAndAllows() {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setRpId("my.jans.server");
		registrationData.setOrigin("https://my.jans.server");
		registrationData.setPublicKeyId("cred-123");

		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", registrationData, "cross-platform", null);

		assertEquals("fido2", event.getService());
		assertEquals("fido2_authentication", event.getEventType());
		assertEquals("authenticate", event.getAction());
		assertEquals("alice", event.getPrincipalId());
		assertEquals("info", event.getSeverityLevel());
		assertEquals("ALLOW", event.getDecisionResult());
		assertNotNull(event.getEventTime());

		assertEquals("my.jans.server", event.getContextInformation().get("rpId"));
		assertEquals("https://my.jans.server", event.getContextInformation().get("origin"));
		assertEquals("cred-123", event.getContextInformation().get("credentialId"));
		assertEquals("cross-platform", event.getContextInformation().get("authenticatorAttachment"));
	}

	/**
	 * A failure can occur before the credential/registration lookup, so this must not throw or
	 * silently swallow the event just because {@code registrationData} is {@code null}.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureBeforeRegistrationDataExists_stillProducesADenyEvent() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent(null, null, null, new IllegalStateException("boom"));

		assertEquals("warning", event.getSeverityLevel());
		assertEquals("DENY", event.getDecisionResult());
		assertNull(event.getPrincipalId());
		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
	}

	/**
	 * Several failure paths in {@code verify()} embed the challenge, credential ID, or user handle
	 * directly in the exception message (e.g. "Couldn't find the key by PublicKeyId '...'") — the
	 * audit trail must never duplicate that beyond what {@code principalId} already carries.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailure_neverIncludesTheExceptionMessage() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", null, null,
				new IllegalStateException("Couldn't find the key by PublicKeyId 'abc123'"));

		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
		event.getContextInformation().values().forEach(value -> {
			if (value.contains("abc123")) {
				throw new AssertionError("audit event context leaked the exception message: " + value);
			}
		});
	}
}
