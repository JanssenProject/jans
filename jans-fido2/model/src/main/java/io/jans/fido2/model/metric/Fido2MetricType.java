/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

/**
 * FIDO2-specific metric types for tracking passkey operations
 *
 * @author Janssen Project
 * @version 1.0
 */
public enum Fido2MetricType {

    // FIDO2/Passkey Registration Metrics
    FIDO2_REGISTRATION_ATTEMPT("fido2_registration_attempt", "Count passkey registration attempts"),
    FIDO2_REGISTRATION_SUCCESS("fido2_registration_success", "Count successful passkey registrations"),
    FIDO2_REGISTRATION_FAILURE("fido2_registration_failure", "Count failed passkey registrations"),
    FIDO2_REGISTRATION_DURATION("fido2_registration_duration", "Passkey registration completion time"),

    // FIDO2/Passkey Authentication Metrics
    FIDO2_AUTHENTICATION_ATTEMPT("fido2_authentication_attempt", "Count passkey authentication attempts"),
    FIDO2_AUTHENTICATION_SUCCESS("fido2_authentication_success", "Count successful passkey authentications"),
    FIDO2_AUTHENTICATION_FAILURE("fido2_authentication_failure", "Count failed passkey authentications"),
    FIDO2_AUTHENTICATION_DURATION("fido2_authentication_duration", "Passkey authentication completion time"),

    // FIDO2/Passkey Fallback Metrics
    FIDO2_FALLBACK_EVENT("fido2_fallback_event", "Count passkey fallback events to other methods"),

    // FIDO2/Passkey Device Analytics
    FIDO2_DEVICE_TYPE_USAGE("fido2_device_type_usage", "Count usage by device/authenticator type");

    private final String metricName;
    private final String description;

    Fido2MetricType(String metricName, String description) {
        this.metricName = metricName;
        this.description = description;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return metricName;
    }
}
