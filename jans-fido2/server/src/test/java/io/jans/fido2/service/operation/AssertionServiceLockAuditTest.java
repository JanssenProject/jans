/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import org.junit.jupiter.api.Test;

import io.jans.fido2.model.audit.LockAuditEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the mapping from an authentication outcome onto the Lock Server audit-event wire shape
 * ({@code AssertionService#buildAuthenticationAuditEvent}), independent of {@code verify()}'s full
 * dependency graph (authentication persistence, assertion verification, external scripts, ...), none
 * of which this mapping touches.
 * <p>
 * {@code verify()}'s own decisions about which {@code rpId}/{@code credentialId}/{@code origin}/
 * {@code failure} values to pass — including the "persisted as authenticated by this invocation" case
 * where a post-persistence exception must still map to ALLOW, not DENY — are not covered here, since
 * they live in {@code verify()} itself, which has no full unit test in this codebase for its happy
 * path (see {@code AssertionServiceTest#verify_ifCeremonyAlreadyTerminal_collectsADenyLockAuditEvent}
 * for that specific branch). This file only proves the mapper does the right thing with whatever it
 * is given.
 */
class AssertionServiceLockAuditTest {

	private final AssertionService assertionService = new AssertionService();

	@Test
	void buildAuthenticationAuditEvent_onSuccess_carriesRpIdAndCredentialIdAndAllows() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", "my.jans.server", "cred-123",
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
	 * A failure can occur before the registration lookup that would otherwise supply rpId/credentialId
	 * even runs (e.g. the challenge itself does not resolve to an entry), so this must not throw or
	 * silently swallow the event just because those values are {@code null}.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureBeforeIdentifiersExist_stillProducesADenyEvent() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent(null, null, null, null, null,
				new IllegalStateException("boom"));

		assertEquals("warning", event.getSeverityLevel());
		assertEquals("DENY", event.getDecisionResult());
		assertNull(event.getPrincipalId());
		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
	}

	/**
	 * rpId and credentialId are sourced independently of any registration lookup (the resolved
	 * ceremony entry and the client-asserted key ID respectively), so a failure raised when that
	 * lookup itself is what failed (e.g. {@code findByPublicKeyId} throwing) must still carry both —
	 * this is the CodeRabbit-flagged gap: previously these identifiers were read off the
	 * {@code Fido2RegistrationData} the failed lookup would have produced, so a DENY event for that
	 * exact failure silently lost both of them.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureFromLookupItself_stillCarriesRpIdAndCredentialId() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", "my.jans.server", "cred-123",
				null, "platform", new IllegalStateException("boom"));

		assertEquals("DENY", event.getDecisionResult());
		assertEquals("my.jans.server", event.getContextInformation().get("rpId"));
		assertEquals("cred-123", event.getContextInformation().get("credentialId"));
	}

	/**
	 * A failure raised after domain verification succeeds (assertion verification, signature checks,
	 * etc. all happen after that point) must not discard the origin context just because the outcome
	 * is a failure — {@code verify()} passes whatever it actually has at the point of failure, not
	 * unconditionally {@code null}.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailureAfterOriginVerified_preservesOrigin() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", "my.jans.server", "cred-123",
				"https://my.jans.server", "platform", new IllegalStateException("boom"));

		assertEquals("DENY", event.getDecisionResult());
		assertEquals("https://my.jans.server", event.getContextInformation().get("origin"));
	}

	/**
	 * The audit trail must not duplicate identifying detail already carried in exception messages
	 * elsewhere in this class (challenge, username) — only the exception's class name is recorded.
	 */
	@Test
	void buildAuthenticationAuditEvent_onFailure_neverIncludesTheExceptionMessage() {
		LockAuditEvent event = assertionService.buildAuthenticationAuditEvent("alice", null, null, null, null,
				new IllegalStateException("Challenge in clientData does not match 'abc123'"));

		assertEquals("IllegalStateException", event.getContextInformation().get("failureReason"));
		event.getContextInformation().values().forEach(value -> {
			if (value.contains("abc123")) {
				throw new AssertionError("audit event context leaked the exception message: " + value);
			}
		});
	}
}
