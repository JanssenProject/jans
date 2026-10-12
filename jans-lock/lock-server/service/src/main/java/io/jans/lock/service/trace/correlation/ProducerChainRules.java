/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;

/**
 * Pure predecessor/peer/successor/lateness logic for {@link TraceCorrelationService}, unit
 * testable without a {@code TraceStore} (design §8, §12, design decisions D-9, D-11). Every method
 * here takes already-fetched rows; none of them perform a lookup.
 *
 * @author Yuriy Movchan
 */
public final class ProducerChainRules {

	private ProducerChainRules() {
	}

	/**
	 * Design §8: a non-genesis position with no predecessor row is an out-of-order arrival, not an
	 * error. Genesis ({@code sequenceNumber == 1}) never has a coverage gap.
	 */
	public static boolean isCoverageGap(long sequenceNumber, List<StoredTraceRecord> predecessors) {
		return sequenceNumber > 1 && predecessors.isEmpty();
	}

	/**
	 * Design §8: a non-genesis position whose predecessor row(s) exist but whose content digest
	 * never matches {@code prevRecordHash} signals a broken chain link. When several predecessors
	 * exist (equivocation at {@code sequenceNumber - 1}), any one matching digest counts as linked.
	 */
	public static boolean isChainLinkFailure(long sequenceNumber, List<StoredTraceRecord> predecessors,
			String prevRecordHash) {
		if (sequenceNumber <= 1 || predecessors.isEmpty()) {
			return false;
		}
		for (StoredTraceRecord predecessor : predecessors) {
			if (constantTimeEquals(predecessor.getContentDigest(), prevRecordHash)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Design decision D-9: every row already occupying this record's chain position, other than one
	 * sharing this record's own identity, is an equivocation peer. (In practice no row here ever
	 * shares the identity — idempotency is handled by the ingestion service before planning — but
	 * the filter is kept so this method stays correct on its own.)
	 */
	public static List<RecordIdentity> equivocationPeers(RecordIdentity thisIdentity,
			List<StoredTraceRecord> rowsAtPosition) {
		List<RecordIdentity> peers = new ArrayList<>();
		for (StoredTraceRecord row : rowsAtPosition) {
			if (!row.getIdentity().equals(thisIdentity)) {
				peers.add(row.getIdentity());
			}
		}
		return peers.isEmpty() ? Collections.emptyList() : peers;
	}

	/**
	 * Design decision D-11: {@code signed_at} is producer time, used only for lateness, never for
	 * ordering. A {@code signed_at} in the future is never late (clock skew).
	 */
	public static boolean isLate(long receivedAtMs, long signedAtSeconds, int latenessThresholdSeconds) {
		return (receivedAtMs / 1000 - signedAtSeconds) > latenessThresholdSeconds;
	}

	/**
	 * Design §8: a successor's {@code prev_record_hash} must equal the just-inserted record's
	 * {@code content_digest}; a mismatch is a chain-link failure that the successor was not able to
	 * detect at its own insertion time (out-of-order arrival).
	 */
	public static boolean successorLinkBroken(String successorPrevRecordHash, String insertedContentDigest) {
		return !constantTimeEquals(successorPrevRecordHash, insertedContentDigest);
	}

	/**
	 * Constant-time equality for the {@code sha256:<hex>} hash strings compared throughout
	 * correlation, so a timing side channel never leaks how much of a hash matched.
	 */
	public static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return Objects.equals(a, b);
		}
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

}
