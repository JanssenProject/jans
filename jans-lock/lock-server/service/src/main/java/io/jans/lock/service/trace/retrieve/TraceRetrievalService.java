/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.retrieve;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.store.TraceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Domain-scoped TRACE record/execution retrieval (design §11.2, §11.3, task 21), extracted out of
 * {@code TraceRestWebServiceImpl} (task 22) so the acceptance suite's {@code TraceTestHarness} can
 * call the exact same logic the REST layer uses instead of re-implementing bare-id ambiguity and
 * domain scoping.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceRetrievalService {

	public static final String REASON_RECORD_NOT_FOUND = "record_not_found";

	public static final String REASON_EXECUTION_NOT_FOUND = "execution_not_found";

	public static final String REASON_MULTIPLE_PRODUCERS = "multiple_producers";

	public static final String REASON_MULTIPLE_AUTHORITIES = "multiple_authorities";

	@Inject
	private TraceStore traceStore;

	/**
	 * @throws TraceValidationException {@code record_not_found} (404) when nothing matches
	 * @throws TraceConflictException   {@code ambiguous_identifier} (409) when {@code producerId} is
	 *                                  blank and more than one producer owns {@code recordId} in the
	 *                                  domain
	 */
	public StoredTraceRecord findRecord(String domainId, String recordId, String producerId) {
		if (StringUtils.isNotBlank(producerId)) {
			RecordIdentity identity = new RecordIdentity(domainId, producerId, recordId);
			return traceStore.findRecord(identity).orElseThrow(
					() -> new TraceValidationException(TraceErrorResponseType.RECORD_NOT_FOUND, REASON_RECORD_NOT_FOUND));
		}

		List<StoredTraceRecord> candidates = traceStore.findRecordsByBareRecordId(domainId, recordId);
		if (candidates.isEmpty()) {
			throw new TraceValidationException(TraceErrorResponseType.RECORD_NOT_FOUND, REASON_RECORD_NOT_FOUND);
		}
		long distinctProducers = candidates.stream().map(r -> r.getIdentity().getProducerId()).distinct().count();
		if (distinctProducers > 1) {
			throw new TraceConflictException(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, REASON_MULTIPLE_PRODUCERS);
		}
		return candidates.get(0);
	}

	/**
	 * @param executionAuthority the {@code execution_authority} query parameter, or blank to resolve
	 *                           it from the domain's records
	 * @throws TraceValidationException {@code execution_not_found} (404) when nothing matches
	 * @throws TraceConflictException   {@code ambiguous_identifier} (409) when {@code executionAuthority}
	 *                                  is blank and more than one authority owns
	 *                                  {@code traceExecutionId} in the domain
	 */
	public ExecutionResult getExecution(String domainId, String traceExecutionId, String executionAuthority, int start,
			int count) {
		String authority = StringUtils.isNotBlank(executionAuthority) ? executionAuthority
				: resolveSoleAuthority(domainId, traceExecutionId);

		ExecutionIdentity exec = new ExecutionIdentity(domainId, authority, traceExecutionId);
		List<StoredTraceRecord> records = traceStore.findRecordsByExecution(exec, start, count);
		if (records.isEmpty() && start == 0) {
			throw new TraceValidationException(TraceErrorResponseType.EXECUTION_NOT_FOUND, REASON_EXECUTION_NOT_FOUND);
		}
		return new ExecutionResult(authority, records);
	}

	private String resolveSoleAuthority(String domainId, String traceExecutionId) {
		List<String> authorities = traceStore.findExecutionAuthorities(domainId, traceExecutionId);
		if (authorities.isEmpty()) {
			throw new TraceValidationException(TraceErrorResponseType.EXECUTION_NOT_FOUND, REASON_EXECUTION_NOT_FOUND);
		}
		if (authorities.size() > 1) {
			throw new TraceConflictException(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, REASON_MULTIPLE_AUTHORITIES);
		}
		return authorities.get(0);
	}

	/** The resolved {@code execution_authority} together with its records in receipt order. */
	public static final class ExecutionResult {

		private final String executionAuthority;

		private final List<StoredTraceRecord> records;

		public ExecutionResult(String executionAuthority, List<StoredTraceRecord> records) {
			this.executionAuthority = executionAuthority;
			this.records = records;
		}

		public String getExecutionAuthority() {
			return executionAuthority;
		}

		public List<StoredTraceRecord> getRecords() {
			return records;
		}

	}

}
