/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.trace.canon.CanonicalHashes;

/**
 * The receipt-hash function of the receipt chain (design §9, TRACE MVP design decision D-8 step 3,
 * D-13):
 *
 * <pre>
 * receiptHash = "sha256:" + hex(SHA-256(JCS({
 *     "evidence_domain_id": domainId,
 *     "receipt_sequence":   receiptSequence,   // JSON integer
 *     "received_at":        receivedAtMs,      // JSON integer, epoch milliseconds
 *     "producer_id":        producerId,
 *     "record_id":          recordId,
 *     "content_digest":     contentDigest,
 *     "prev_receipt_hash":  prevReceiptHash
 * })))
 * </pre>
 *
 * Member order above does not matter to the result: JCS (RFC 8785) sorts object members by key
 * before serializing, so any verifier that builds the same seven members reproduces the same
 * hash regardless of construction order.
 *
 * @author Yuriy Movchan
 */
public final class ReceiptHashes {

	private ReceiptHashes() {
	}

	/**
	 * @return the {@code "sha256:<64 hex>"} receipt hash for one receipt-chain position
	 */
	public static String receiptHash(String domainId, long receiptSequence, long receivedAtMs, String producerId,
			String recordId, String contentDigest, String prevReceiptHash) {
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(producerId, "producerId");
		Objects.requireNonNull(recordId, "recordId");
		Objects.requireNonNull(contentDigest, "contentDigest");
		Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");

		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("evidence_domain_id", domainId);
		node.put("receipt_sequence", receiptSequence);
		node.put("received_at", receivedAtMs);
		node.put("producer_id", producerId);
		node.put("record_id", recordId);
		node.put("content_digest", contentDigest);
		node.put("prev_receipt_hash", prevReceiptHash);
		return CanonicalHashes.sha256PrefixedOfJcs(node);
	}

}
