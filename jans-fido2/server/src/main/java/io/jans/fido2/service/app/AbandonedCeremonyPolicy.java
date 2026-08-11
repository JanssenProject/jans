/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import io.jans.fido2.model.conf.Fido2Configuration;

/**
 * Resolves the timing the abandonment sweep depends on.
 * <p>
 * The sweep and the code that writes pending ceremonies have to agree on two derived values, and they
 * live in different classes. Deriving both here keeps the invariant in one place: a pending ceremony
 * must survive long enough for at least one sweep to claim it after its window elapses, but no longer
 * than necessary.
 *
 * @author Janssen Project
 */
public final class AbandonedCeremonyPolicy {

	private static final int MINIMUM_INTERVAL = 1;

	private AbandonedCeremonyPolicy() {
	}

	/**
	 * The interval the sweep actually runs at.
	 * <p>
	 * A configured interval at or above {@code unfinishedRequestExpiration} is not usable: a ceremony
	 * could lapse and be deleted entirely between two passes. Such a value is capped rather than
	 * honoured, because silently sweeping too slowly loses data while running more often does not.
	 *
	 * @return a positive interval in seconds, strictly below the ceremony window whenever that window
	 *         itself leaves room for one
	 */
	public static int effectiveSweepInterval(Fido2Configuration fido2Configuration) {
		int unfinishedRequestExpiration = fido2Configuration.getUnfinishedRequestExpiration();
		int configured = fido2Configuration.getAbandonedRequestSweepInterval();

		// Half the window guarantees a pass inside it while staying strictly below it. Never below
		// MINIMUM_INTERVAL, so a nonsensically short window still yields a runnable schedule.
		int maximumUsable = Math.max(MINIMUM_INTERVAL, unfinishedRequestExpiration / 2);

		if (configured < MINIMUM_INTERVAL || configured > maximumUsable) {
			return maximumUsable;
		}
		return configured;
	}

	/**
	 * True when the configured interval had to be overridden, so the caller can say so once at startup
	 * rather than on every pass.
	 */
	public static boolean isSweepIntervalOverridden(Fido2Configuration fido2Configuration) {
		return fido2Configuration.getAbandonedRequestSweepInterval() != effectiveSweepInterval(fido2Configuration);
	}

	/**
	 * How long a {@code pending} ceremony is retained.
	 * <p>
	 * The row has to outlive its own ceremony window, otherwise it becomes eligible for deletion at the
	 * same instant the sweep becomes eligible to claim it and abandonment goes unrecorded. Two sweep
	 * intervals of grace guarantees a pass falls inside it. This does not widen the window in which an
	 * assertion is accepted — that is enforced against the ceremony's issue time when it is verified.
	 * <p>
	 * With the sweep disabled the retention is exactly what it always was, so nothing changes for a
	 * deployment that has opted out.
	 */
	public static int pendingCeremonyRetention(Fido2Configuration fido2Configuration) {
		int unfinishedRequestExpiration = fido2Configuration.getUnfinishedRequestExpiration();
		if (!fido2Configuration.isRecordAbandonedAssertions()) {
			return unfinishedRequestExpiration;
		}
		return unfinishedRequestExpiration + (2 * effectiveSweepInterval(fido2Configuration));
	}
}
