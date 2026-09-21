/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * Enforces the design §7.2 schema for one {@code trace.event_kind} and extracts the values the
 * correlation service (task 17) indexes into the shared {@link CorrelationInputs.Builder}.
 * Common-field validation (design §7.1) has already run by the time {@link #validate} is called;
 * this is only the per-kind {@code trace.event} shape and the kind-contradicting members listed
 * in design §7.2.
 *
 * @author Yuriy Movchan
 */
public interface EventKindValidator {

	/**
	 * @return the {@code trace.event_kind} value this validator handles (one of the
	 *         {@code TraceConstants.EVENT_KIND_*} constants)
	 */
	String kind();

	/**
	 * @param assertion the common-field-validated assertion; must not be mutated
	 * @param out the correlation-inputs builder to populate with this kind's extracted values
	 * @throws io.jans.lock.service.trace.parse.TraceValidationException on any §7.2/§7.3 violation
	 */
	void validate(ParsedAssertion assertion, CorrelationInputs.Builder out);

}
