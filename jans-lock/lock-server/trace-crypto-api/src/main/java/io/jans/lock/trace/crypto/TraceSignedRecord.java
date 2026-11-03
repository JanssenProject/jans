/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A parsed signed TRACE record handed to {@link TraceCryptoProvider#verifyAndDigest}: the exact
 * text received, its parsed root object and the two identifiers needed to select the verification
 * key. The class never prints record content.
 *
 * @author Yuriy Movchan
 */
public final class TraceSignedRecord {

	private final String rawText;

	private final ObjectNode root;

	private final String producer;

	private final String kid;

	public TraceSignedRecord(String rawText, ObjectNode root, String producer, String kid) {
		this.rawText = Objects.requireNonNull(rawText, "rawText");
		this.root = Objects.requireNonNull(root, "root");
		this.producer = Objects.requireNonNull(producer, "producer");
		this.kid = Objects.requireNonNull(kid, "kid");
	}

	/**
	 * @return the record text exactly as received
	 */
	public String getRawText() {
		return rawText;
	}

	/**
	 * @return the parsed record, including the top-level {@code signature} member
	 */
	public ObjectNode getRoot() {
		return root;
	}

	/**
	 * @return the producer id of the record
	 */
	public String getProducer() {
		return producer;
	}

	/**
	 * @return the key id the record names
	 */
	public String getKid() {
		return kid;
	}

}
