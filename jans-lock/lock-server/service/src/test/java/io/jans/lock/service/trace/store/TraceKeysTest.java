/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * Tests for {@link TraceKeys}: design decision D-6. Not run under TestNG — see the project note
 * that Surefire in {@code lock-server} silently skips TestNG classes; JUnit 5 is used for every
 * TRACE test in this series (established in task 02).
 */
class TraceKeysTest {

	private static final String DOMAIN = "default";

	@Test
	void testRecordKey_isStable64Hex() {
		String key = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-1"));

		assertEquals(64, key.length());
		assertTrue(key.matches("^[0-9a-f]{64}$"));
		assertEquals(key, TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-1")));
	}

	@Test
	void testRecordKey_differentComponents_differentKeys() {
		String a = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-1"));
		String b = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-2"));

		assertNotEquals(a, b);
	}

	@Test
	void testRecordKey_separatorInjection_noCollision() {
		// "a|b","c" vs "a","b|c" — the classic separator-collision probe. JCS array serialization
		// keeps element boundaries unambiguous regardless of what characters a component contains.
		String withPipeInFirst = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "a|b", "c"));
		String withPipeInSecond = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "a", "b|c"));

		assertNotEquals(withPipeInFirst, withPipeInSecond);
	}

	@Test
	void testCapabilityKey_vs_tokenKey_typeConfusion_noCollision() {
		// Same domain and same literal component value, but different key "type" tag ("cap" vs
		// "tok") and different token-ref shape must never collide.
		String capKey = TraceKeys.capabilityKey(DOMAIN, "shared-value");
		String tokKey = TraceKeys.tokenKey(DOMAIN, new TokenRef("shared-value", "access_token", "jti", null));

		assertNotEquals(capKey, tokKey);
	}

	@Test
	void testTokenKey_jtiBranch_vs_fingerprintBranch_noCollision() {
		String jtiKey = TraceKeys.tokenKey(DOMAIN, new TokenRef("issuer-1", "access_token", "x", null));
		String fpKey = TraceKeys.tokenKey(DOMAIN, new TokenRef("issuer-1", "access_token", null, "x"));

		assertNotEquals(jtiKey, fpKey);
	}

	@Test
	void testTokenKey_ignoresTokenType() {
		String withTypeA = TraceKeys.tokenKey(DOMAIN, new TokenRef("issuer-1", "access_token", "jti-1", null));
		String withTypeB = TraceKeys.tokenKey(DOMAIN, new TokenRef("issuer-1", "refresh_token", "jti-1", null));

		assertEquals(withTypeA, withTypeB);
	}

	@Test
	void testChainPositionKey_sequenceNumberIsDecimalStringComponent() {
		ChainIdentity chain = new ChainIdentity(DOMAIN, "producer-1", "instance-1", "chain-1");
		String seq1 = TraceKeys.chainPositionKey(new ChainPosition(chain, 1L));
		String seq2 = TraceKeys.chainPositionKey(new ChainPosition(chain, 2L));
		String seq21 = TraceKeys.chainPositionKey(new ChainPosition(chain, 21L));

		assertNotEquals(seq1, seq2);
		// "2" followed by "1" as a decimal string must not collide with the literal digit "21".
		assertNotEquals(seq2, seq21);
	}

	@Test
	void testReceiptKey_sequenceAsDecimalString_noNumberVsStringCollision() {
		String key = TraceKeys.receiptKey(DOMAIN, 108422L);

		assertEquals(64, key.length());
		assertEquals(key, TraceKeys.receiptKey(DOMAIN, 108422L));
	}

	@Test
	void testExecutionKey_vs_recordKey_typeConfusion_noCollision() {
		String execKey = TraceKeys.executionKey(new ExecutionIdentity(DOMAIN, "a", "b"));
		String recKey = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "a", "b"));

		assertNotEquals(execKey, recKey);
	}

	@Test
	void testCapabilityKey_nullComponent_throws() {
		// Reaches TraceKeys.key(...)'s own Objects.requireNonNull directly, unlike constructing an
		// identity value type with a null field (which is rejected one layer earlier).
		assertThrows(NullPointerException.class, () -> TraceKeys.capabilityKey(DOMAIN, null));
	}

	@Test
	void testRecordDn_lengthWithinLimit_defaultBaseDn() {
		String baseDn = "ou=trace,ou=lock,o=jans";
		String key = TraceKeys.recordKey(new RecordIdentity(DOMAIN, "producer-1", "record-1"));

		String recordDn = TraceKeys.recordDn(baseDn, key);
		String receiptDn = TraceKeys.receiptDn(baseDn, key);
		String chainDn = TraceKeys.chainDn(baseDn, key);
		String producerKeyDn = TraceKeys.producerKeyDn(baseDn, key);

		assertEquals("jansId=" + key + ",ou=records," + baseDn, recordDn);
		assertTrue(recordDn.length() <= 128, "record DN length " + recordDn.length());
		assertTrue(receiptDn.length() <= 128, "receipt DN length " + receiptDn.length());
		assertTrue(chainDn.length() <= 128, "chain DN length " + chainDn.length());
		assertTrue(producerKeyDn.length() <= 128, "producer key DN length " + producerKeyDn.length());
	}

}
