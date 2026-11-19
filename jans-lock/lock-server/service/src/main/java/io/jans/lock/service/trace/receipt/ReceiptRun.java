/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;

/**
 * A contiguous run of receipt positions computed by {@link TraceReceiptChain#runWithRange} for one
 * attempt: the {@code COMMITTED} receipt rows in chain order, ready to be stored together with the
 * records, and, for each input item (same order), the {@link Position} it received.
 *
 * <p>The rows are fresh objects for every attempt, so an action may hand them to the store, which
 * sets their {@code id}/{@code dn}.
 *
 * @author Yuriy Movchan
 */
public final class ReceiptRun {

	/** The {@code (seq, prevReceiptHash, receiptHash)} of one item's receipt. */
	public static final class Position {

		private final long seq;

		private final String prevReceiptHash;

		private final String receiptHash;

		Position(long seq, String prevReceiptHash, String receiptHash) {
			this.seq = seq;
			this.prevReceiptHash = Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");
			this.receiptHash = Objects.requireNonNull(receiptHash, "receiptHash");
		}

		public long getSeq() {
			return seq;
		}

		public String getPrevReceiptHash() {
			return prevReceiptHash;
		}

		public String getReceiptHash() {
			return receiptHash;
		}

	}

	private final String domainId;

	private final int attempt;

	private final List<ReceiptBatchItem> items;

	private final List<TraceReceiptEntry> rows;

	private final List<Position> positions;

	ReceiptRun(String domainId, int attempt, List<ReceiptBatchItem> items, List<TraceReceiptEntry> rows) {
		this.domainId = domainId;
		this.attempt = attempt;
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
		this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
		List<Position> list = new ArrayList<>(rows.size());
		for (TraceReceiptEntry row : rows) {
			list.add(new Position(row.getReceiptSeq(), row.getPrevReceiptHash(), row.getReceiptHash()));
		}
		this.positions = Collections.unmodifiableList(list);
	}

	/** @return a run with no items and no rows, which {@code runWithRange} returns when nothing was written */
	static ReceiptRun empty(String domainId, int attempt) {
		return new ReceiptRun(domainId, attempt, Collections.<ReceiptBatchItem>emptyList(),
				Collections.<TraceReceiptEntry>emptyList());
	}

	public String getDomainId() {
		return domainId;
	}

	/** @return the 0-based attempt number that produced this run; above 0 after a position collision */
	public int getAttempt() {
		return attempt;
	}

	/** @return the items this run was computed for, in chain order */
	public List<ReceiptBatchItem> getItems() {
		return items;
	}

	/** @return the {@code COMMITTED} receipt rows in chain order */
	public List<TraceReceiptEntry> getRows() {
		return rows;
	}

	/** @return the position of each item, in the order of {@link #getItems()} */
	public List<Position> getPositions() {
		return positions;
	}

	public boolean isEmpty() {
		return rows.isEmpty();
	}

}
