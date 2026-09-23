/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

/**
 * Fires the periodic flush of buffered Lock Server audit events. Deliberately its own timer/event
 * rather than piggybacking on an existing one (e.g. {@code MetricService}'s), since the flush
 * cadence (~15-30s) is a delivery-pattern requirement from the Lock Server side, not tied to any
 * other subsystem's schedule.
 */
public interface LockAuditFlushEvent {

}
