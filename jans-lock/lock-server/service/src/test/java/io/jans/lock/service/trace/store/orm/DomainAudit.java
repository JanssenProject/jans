/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ChainVerificationReport;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.orm.PersistenceEntryManager;

/**
 * The correctness checks of a domain after a concurrent ingest run (task 08, deliverable 2): the
 * receipt sequences are {@code 1..N} with no gap and no duplicate, every receipt is
 * {@code COMMITTED} (no {@code PENDING} row is left), the number of records equals the number of
 * accepted outcomes, every receipt has exactly one record which carries its identity, sequence and
 * digest, and {@link TraceReceiptChain#verifyDomain} reports no problem. Reads go through the
 * multi-key store methods in chunks of 100, so a domain of tens of thousands of receipts is audited
 * in seconds; {@code verifyDomain} is optional because it reads two rows per position.
 *
 * @author Yuriy Movchan
 */
final class DomainAudit {

	private static final int CHUNK = 100;

	private final TraceStore store;

	private final PersistenceEntryManager entryManager;

	private final String domain;

	private final List<String> problems = new ArrayList<>();

	private long head;

	private int receipts;

	private int records;

	DomainAudit(TraceStore store, PersistenceEntryManager entryManager, String domain) {
		this.store = store;
		this.entryManager = entryManager;
		this.domain = domain;
	}

	/**
	 * @param expectedAccepted the number of accepted, non-replay outcomes of the run(s); {@code -1} skips the count check
	 * @param walkChain        also run {@code verifyDomain} over the whole domain
	 * @return this audit; {@link #problems()} is empty when everything holds
	 */
	DomainAudit run(long expectedAccepted, boolean walkChain, TraceReceiptChain chain) {
		head = store.findReceiptHead(domain).map(h -> h.getReceiptSequence()).orElse(0L);
		receipts = DatabaseProfile.countReceipts(entryManager, domain);
		records = (int) store.countRecords(domain);
		int pending = DatabaseProfile.countReceipts(entryManager, domain, TraceReceiptState.PENDING.name());
		int committed = DatabaseProfile.countReceipts(entryManager, domain, TraceReceiptState.COMMITTED.name());

		if (receipts != head) {
			problems.add("receipt rows " + receipts + " but head sequence " + head + ": gap or duplicate");
		}
		if (pending != 0) {
			problems.add(pending + " PENDING receipt rows left");
		}
		if (committed != receipts) {
			problems.add("only " + committed + " of " + receipts + " receipts are COMMITTED");
		}
		if (expectedAccepted >= 0 && records != expectedAccepted) {
			problems.add("record rows " + records + " but accepted outcomes " + expectedAccepted);
		}
		if (records != head) {
			problems.add("record rows " + records + " but head sequence " + head);
		}

		Map<RecordIdentity, Long> seqByIdentity = new HashMap<>();
		for (long from = 1; from <= head; from += CHUNK) {
			List<Long> sequences = new ArrayList<>();
			for (long seq = from; seq < from + CHUNK && seq <= head; seq++) {
				sequences.add(seq);
			}
			List<TraceReceiptEntry> rows = store.findReceipts(domain, sequences);
			if (rows.size() != sequences.size()) {
				problems.add("positions " + from + ".." + sequences.get(sequences.size() - 1) + ": " + rows.size()
						+ " receipts for " + sequences.size() + " sequences");
			}
			List<RecordIdentity> identities = new ArrayList<>();
			Map<RecordIdentity, TraceReceiptEntry> receiptByIdentity = new HashMap<>();
			for (TraceReceiptEntry row : rows) {
				RecordIdentity identity = new RecordIdentity(domain, row.getProducerId(), row.getRecordId());
				if (seqByIdentity.put(identity, row.getReceiptSeq()) != null) {
					problems.add("identity " + identity + " has more than one receipt row");
				}
				if (row.getReceiptStateEnum() != TraceReceiptState.COMMITTED) {
					problems.add("receipt " + row.getReceiptSeq() + " is " + row.getReceiptStateEnum());
				}
				identities.add(identity);
				receiptByIdentity.put(identity, row);
			}
			Map<RecordIdentity, StoredTraceRecord> found = store.findRecords(identities);
			for (RecordIdentity identity : identities) {
				TraceReceiptEntry receipt = receiptByIdentity.get(identity);
				StoredTraceRecord record = found.get(identity);
				if (record == null) {
					problems.add("receipt " + receipt.getReceiptSeq() + " has no record " + identity);
				} else if (!TraceReceiptChain.carriesClaim(record, receipt.getReceiptSeq(), receipt.getContentDigest())) {
					problems.add("receipt " + receipt.getReceiptSeq() + " and record " + identity
							+ " disagree: record seq " + record.getReceipt().getReceiptSequence());
				}
			}
		}

		if (walkChain && chain != null) {
			ChainVerificationReport report = chain.verifyDomain(domain, Integer.MAX_VALUE);
			if (!report.isOk()) {
				problems.add("verifyDomain: " + report);
			}
			if (report.getHeadSequence() != head) {
				problems.add("verifyDomain head " + report.getHeadSequence() + " differs from " + head);
			}
		}
		return this;
	}

	List<String> problems() {
		return problems;
	}

	boolean ok() {
		return problems.isEmpty();
	}

	long head() {
		return head;
	}

	int receipts() {
		return receipts;
	}

	int records() {
		return records;
	}

	/** @return the record stored under {@code identity}, for flag comparisons */
	Optional<StoredTraceRecord> record(RecordIdentity identity) {
		return store.findRecord(identity);
	}

	@Override
	public String toString() {
		return "DomainAudit[domain=" + domain + ", head=" + head + ", receipts=" + receipts + ", records=" + records
				+ ", problems=" + problems + "]";
	}

}
