/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.List;

import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.StoredTraceRecord;

/**
 * What {@link TraceCorrelationService#plan} reads about records already stored, besides the chain
 * registry and the policy check. The single-record path answers from the {@code TraceStore}; the
 * batch path answers from {@link BatchOverlay}, which also knows the earlier records of the same batch.
 *
 * @author Yuriy Movchan
 */
public interface PlanLookup {

	/**
	 * @return every record at this chain position, ordered by receipt sequence; never {@code null}
	 */
	List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position);

	/**
	 * @return {@code true} when this lookup already saw {@code chain} pass the registry check, so
	 *         {@code plan} may skip the registry call for it; the single-record path never does
	 */
	default boolean isChainConfirmed(ChainIdentity chain) {
		return false;
	}

	/** Called by {@code plan} after the registry check for {@code chain} succeeded. */
	default void confirmChain(ChainIdentity chain) {
		// no memo by default
	}

}
