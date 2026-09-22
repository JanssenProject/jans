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
 * Pins the mapping from a registration outcome onto the Lock Server audit-event wire shape
 * ({@code AttestationService#buildRegistrationAuditEvent}), independent of {@code verify()}'s full
 * dependency graph (registration persistence, attestation verification, external scripts, ...),
 * none of which this mapping touches.
 */
class AttestationServiceLockAuditTest {

	private final AttestationService attestationService = new AttestationService();

	@Test
	void buildRegistrationAuditEvent_onSuccess_carriesRegistrationDataAndAllows() {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		registrationData.setRpId("my.jans.server");
		registrationData.setOrigin("https://my.jans.server");
		registrationData.setPublicKeyId("cred-123");
		registrationData.setAttestationType("packed");

		LockAuditEvent event = attestationService.buildRegistrationAuditEvent("alice", registrationData, "platform", null);

		assertEquals("fido2", event.getService());
		assertEquals("fido2_registration", event.getEventType());
		assertEquals("register", event.getAction());
		assertEquals("alice", event.getPrincipalId());
		assertEquals("info", event.getSeverityLevel());
		assertEquals("ALLOW", event.getDecisionResult());
		assertNotNull(event.getEventTime());

		assertEquals("my.jans.server", event.getContextInformation().get("rpId"));
		assertEquals("https://my.jans.server", event.getContextInformation().get("origin"));
		assertEquals("cred-123", event.getContextInformation().get("credentialId"));
		assertEquals("packed", event.getContextInformation().get("attestationType"));
		assertEquals("platform", event.getContextInformation().get("authenticatorAttachment"));
	}

	/**
	 * A failure can occur before the registration entry is even looked up, so this must not throw or
	 * silently swallow the event just because {@code registrationData} is {@code null}.
	 */
	@Test
	void buildRegistrationAuditEvent_onFailureBeforeRegistrationDataExists_stillProducesADenyEvent() {
		LockAuditEvent event = attestationService.buildRegistrationAuditEvent(null, null, null, new IllegalStateException("boom"));

		assertEquals("warning", event.getSeverityLevel());
		assertEquals("DENY", event.getDecisionResult());
		assertNull(event.getPrincipalId());
		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
	}

	/**
	 * The audit trail must not duplicate identifying detail already carried in exception messages
	 * elsewhere in this class (challenge, username) — only the exception's class name is recorded.
	 */
	@Test
	void buildRegistrationAuditEvent_onFailure_neverIncludesTheExceptionMessage() {
		LockAuditEvent event = attestationService.buildRegistrationAuditEvent("alice", null, null,
				new IllegalStateException("Can't find associated attestation request by challenge 'abc123'"));

		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
		event.getContextInformation().values().forEach(value -> {
			if (value.contains("abc123")) {
				throw new AssertionError("audit event context leaked the exception message: " + value);
			}
		});
	}
}
