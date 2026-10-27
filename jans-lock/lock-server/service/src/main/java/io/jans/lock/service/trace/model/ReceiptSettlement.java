/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

/**
 * Outcome of settling a receipt-chain claim (design §9). A claim leaves {@code PENDING} exactly once,
 * to {@code COMMITTED} or {@code VOID}; both are terminal. Applying the terminal state that is
 * already stored succeeds without a write, applying the other one changes nothing.
 *
 * @author Yuriy Movchan
 */
public enum ReceiptSettlement {

	/** The claim was {@code PENDING} and now holds the requested terminal state. */
	SETTLED,

	/** The requested terminal state was already stored; nothing was written. */
	ALREADY_SETTLED,

	/** The other terminal state is already stored; nothing was written. */
	CONFLICT,

	/** No receipt exists at this {@code (domainId, receiptSequence)}. */
	NOT_FOUND,

	/**
	 * Receipt-chain level only, never returned by a {@code TraceStore}: the store failed, so the claim
	 * state is unknown and is left for the repair timer.
	 */
	FAILED

}
