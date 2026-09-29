/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * The four Lock-derived ingestion flags on a TRACE record (design §10, T-1). Unlike the other
 * store value types this class is a mutable, copy-able bag: {@code coverageGap},
 * {@code chainLinkFailure} and {@code equivocation} may be updated after ingestion by the
 * correlation service (design decision D-9); {@code late} is fixed at ingestion and the store
 * layer never changes it via {@code updateRecordFlags}.
 *
 * @author Yuriy Movchan
 */
public final class IngestionFlags {

	private boolean coverageGap;

	private boolean chainLinkFailure;

	private boolean equivocation;

	private boolean late;

	public IngestionFlags(boolean coverageGap, boolean chainLinkFailure, boolean equivocation, boolean late) {
		this.coverageGap = coverageGap;
		this.chainLinkFailure = chainLinkFailure;
		this.equivocation = equivocation;
		this.late = late;
	}

	/**
	 * Copy constructor, so callers can hand out an independent instance instead of a shared
	 * mutable reference.
	 */
	public IngestionFlags(IngestionFlags other) {
		this(other.coverageGap, other.chainLinkFailure, other.equivocation, other.late);
	}

	/**
	 * @return a new, independent {@link IngestionFlags} with the same values
	 */
	public IngestionFlags copy() {
		return new IngestionFlags(this);
	}

	public boolean isCoverageGap() {
		return coverageGap;
	}

	public void setCoverageGap(boolean coverageGap) {
		this.coverageGap = coverageGap;
	}

	public boolean isChainLinkFailure() {
		return chainLinkFailure;
	}

	public void setChainLinkFailure(boolean chainLinkFailure) {
		this.chainLinkFailure = chainLinkFailure;
	}

	public boolean isEquivocation() {
		return equivocation;
	}

	public void setEquivocation(boolean equivocation) {
		this.equivocation = equivocation;
	}

	public boolean isLate() {
		return late;
	}

	public void setLate(boolean late) {
		this.late = late;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof IngestionFlags)) {
			return false;
		}
		IngestionFlags other = (IngestionFlags) o;
		return coverageGap == other.coverageGap && chainLinkFailure == other.chainLinkFailure
				&& equivocation == other.equivocation && late == other.late;
	}

	@Override
	public int hashCode() {
		return Objects.hash(coverageGap, chainLinkFailure, equivocation, late);
	}

	@Override
	public String toString() {
		return "IngestionFlags [coverageGap=" + coverageGap + ", chainLinkFailure=" + chainLinkFailure
				+ ", equivocation=" + equivocation + ", late=" + late + "]";
	}

}
