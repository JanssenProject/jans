/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

/**
 * Fires the sweep for assertion ceremonies that lapsed without ever being completed.
 * <p>
 * Deliberately separate from {@code CleanerEvent}: the sweep has to run on its own, shorter cadence
 * so that it can claim a lapsed ceremony before the cleaner deletes it.
 */
public interface AbandonedCeremonyEvent {

}
