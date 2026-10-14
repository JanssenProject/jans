/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * A TRACE execution's external identity: {@code (evidence_domain_id, execution_authority,
 * trace_execution_id)} (design decision D-6, key type {@code exec}).
 *
 * @author Yuriy Movchan
 */
public final class ExecutionIdentity {

	private final String domainId;

	private final String executionAuthority;

	private final String traceExecutionId;

	public ExecutionIdentity(String domainId, String executionAuthority, String traceExecutionId) {
		this.domainId = Objects.requireNonNull(domainId, "domainId");
		this.executionAuthority = Objects.requireNonNull(executionAuthority, "executionAuthority");
		this.traceExecutionId = Objects.requireNonNull(traceExecutionId, "traceExecutionId");
	}

	public String getDomainId() {
		return domainId;
	}

	public String getExecutionAuthority() {
		return executionAuthority;
	}

	public String getTraceExecutionId() {
		return traceExecutionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ExecutionIdentity)) {
			return false;
		}
		ExecutionIdentity other = (ExecutionIdentity) o;
		return domainId.equals(other.domainId) && executionAuthority.equals(other.executionAuthority)
				&& traceExecutionId.equals(other.traceExecutionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domainId, executionAuthority, traceExecutionId);
	}

	@Override
	public String toString() {
		return "ExecutionIdentity [domainId=" + domainId + ", executionAuthority=" + executionAuthority
				+ ", traceExecutionId=" + traceExecutionId + "]";
	}

}
