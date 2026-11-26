/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class TraceRecordBuilderIdentityFieldsTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC);
	private static final String CREDENTIAL_ID = Base64.getUrlEncoder().withoutPadding().encodeToString("cred-1".getBytes());

	private static JsonNode build(boolean includeHash, String attachment, String credentialId) {
		TraceRecordBuilder builder = new TraceRecordBuilder("fido-node-1", "fido-node-1", CLOCK, () -> "rec-1", includeHash);
		TraceRecordBuilder.Ceremony ceremony = new TraceRecordBuilder.Ceremony(TraceRecordBuilder.CeremonyType.REGISTRATION,
				TraceRecordBuilder.Outcome.SUCCESS, "sess-1", "https://issuer.example.com", attachment, credentialId);
		return builder.build(ceremony).path("trace").path("event");
	}

	@Test
	void build_ifFlagOn_carriesAttachmentAndHashNotTheRawCredentialId() {
		JsonNode event = build(true, "platform", CREDENTIAL_ID);

		assertEquals("platform", event.path("authenticator_attachment").asText());
		assertTrue(event.path("credential_id_hash").asText().startsWith("sha256:"));
		assertFalse(event.toString().contains(CREDENTIAL_ID), "raw credential ID must not appear");
	}

	@Test
	void build_ifFlagOff_omitsTheHashButKeepsTheAttachment() {
		JsonNode event = build(false, "cross-platform", CREDENTIAL_ID);

		assertEquals("cross-platform", event.path("authenticator_attachment").asText());
		assertTrue(event.path("credential_id_hash").isMissingNode());
	}

	@Test
	void build_ifAttachmentUnknown_omitsIt() {
		JsonNode event = build(true, "bluetooth", CREDENTIAL_ID);

		assertTrue(event.path("authenticator_attachment").isMissingNode());
	}

	@Test
	void build_ifCredentialIdNotBase64url_omitsTheHashRatherThanGuessing() {
		JsonNode event = build(true, null, "not base64!");

		assertTrue(event.path("credential_id_hash").isMissingNode());
	}
}
