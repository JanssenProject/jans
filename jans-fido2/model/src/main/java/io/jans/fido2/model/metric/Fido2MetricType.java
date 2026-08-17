/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import io.jans.model.metric.MetricData;
import io.jans.model.metric.MetricTypeDeclaration;
import io.jans.model.metric.counter.CounterMetricData;
import io.jans.model.metric.counter.CounterMetricEntry;
import io.jans.model.metric.ldap.MetricEntry;
import io.jans.model.metric.timer.TimerMetricData;
import io.jans.model.metric.timer.TimerMetricEntry;

/**
 * FIDO2-specific metric types for tracking passkey operations
 *
 * @author Janssen Project
 * @version 1.0
 */
public enum Fido2MetricType implements MetricTypeDeclaration {

    // FIDO2/Passkey Registration Metrics
    FIDO2_REGISTRATION_ATTEMPT("fido2_registration_attempt", "Count passkey registration attempts",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_REGISTRATION_SUCCESS("fido2_registration_success", "Count successful passkey registrations",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_REGISTRATION_FAILURE("fido2_registration_failure", "Count failed passkey registrations",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_REGISTRATION_DURATION("fido2_registration_duration", "Passkey registration completion time",
            TimerMetricData.class, TimerMetricEntry.class),

    // FIDO2/Passkey Authentication Metrics
    FIDO2_AUTHENTICATION_ATTEMPT("fido2_authentication_attempt", "Count passkey authentication attempts",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_AUTHENTICATION_SUCCESS("fido2_authentication_success", "Count successful passkey authentications",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_AUTHENTICATION_FAILURE("fido2_authentication_failure", "Count failed passkey authentications",
            CounterMetricData.class, CounterMetricEntry.class),
    FIDO2_AUTHENTICATION_DURATION("fido2_authentication_duration", "Passkey authentication completion time",
            TimerMetricData.class, TimerMetricEntry.class),
    FIDO2_AUTHENTICATION_ABANDONED("fido2_authentication_abandoned",
            "Count passkey authentications started but never completed", CounterMetricData.class,
            CounterMetricEntry.class),

    // FIDO2/Passkey Fallback Metrics
    FIDO2_FALLBACK_EVENT("fido2_fallback_event", "Count passkey fallback events to other methods",
            CounterMetricData.class, CounterMetricEntry.class),

    // FIDO2/Passkey Device Analytics
    FIDO2_DEVICE_TYPE_USAGE("fido2_device_type_usage", "Count usage by device/authenticator type",
            CounterMetricData.class, CounterMetricEntry.class);

    private final String metricName;
    private final String description;
    private final Class<? extends MetricData> eventDataType;
    private final Class<? extends MetricEntry> metricEntryType;

    Fido2MetricType(String metricName, String description, Class<? extends MetricData> eventDataType,
            Class<? extends MetricEntry> metricEntryType) {
        this.metricName = metricName;
        this.description = description;
        this.eventDataType = eventDataType;
        this.metricEntryType = metricEntryType;
    }

    @Override
    public String getValue() {
        return metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public String getDisplayName() {
        return description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends MetricData> getEventDataType() {
        return eventDataType;
    }

    @Override
    public Class<? extends MetricEntry> getMetricEntryType() {
        return metricEntryType;
    }

    @Override
    public String toString() {
        return metricName;
    }
}
