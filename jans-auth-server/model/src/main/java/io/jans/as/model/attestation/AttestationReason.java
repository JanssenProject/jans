/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

/**
 * Stable reason codes of attestation verdicts ({@code reasons}) and of {@code invalid_challenge} errors ({@code reason}).
 */
public final class AttestationReason {

    public static final String MEASUREMENT_NOT_ALLOWED = "measurement_not_allowed";
    public static final String MEASUREMENT_POLICY_NOT_CONFIGURED = "measurement_policy_not_configured";
    public static final String EVIDENCE_EXPIRED = "evidence_expired";
    public static final String NONCE_MISMATCH = "nonce_mismatch";
    public static final String KEY_MISMATCH = "key_mismatch";
    public static final String KEY_NOT_CERTIFIED = "key_not_certified";
    public static final String UNTRUSTED_VERIFIER = "untrusted_verifier";
    public static final String DEBUG_ENABLED = "debug_enabled";
    public static final String VERIFIER_UNAVAILABLE = "verifier_unavailable";
    public static final String NO_VERDICT = "no_verdict";
    public static final String BINDING_UNESTABLISHED = "binding_unestablished";
    public static final String EXPERIMENTAL_PROFILE_REFUSED = "experimental_profile_refused";
    public static final String SCRIPT_REJECTED = "script_rejected";
    public static final String INVALID_EVIDENCE = "invalid_evidence";

    public static final String CHALLENGE_UNKNOWN = "challenge_unknown";
    public static final String CHALLENGE_USED = "challenge_used";
    public static final String CHALLENGE_EXPIRED = "challenge_expired";
    public static final String CHALLENGE_IN_PROGRESS = "challenge_in_progress";

    private AttestationReason() {
    }
}
