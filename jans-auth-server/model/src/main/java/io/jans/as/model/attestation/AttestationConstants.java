/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

/**
 * Request and response member names of the attestation endpoint.
 */
public final class AttestationConstants {

    public static final String ATTESTED_KEY = "attested_key";
    public static final String CLIENT_ID = "client_id";
    public static final String PURPOSE = "purpose";
    public static final String CHALLENGE_ID = "challenge_id";
    public static final String NONCE = "nonce";
    public static final String EXPIRES_IN = "expires_in";
    public static final String EVIDENCE_FORMATS_SUPPORTED = "evidence_formats_supported";
    public static final String EVIDENCE = "evidence";
    public static final String EVIDENCE_FORMAT = "evidence_format";

    public static final String ATTESTATION_ID = "attestation_id";
    public static final String STATUS = "status";
    public static final String ATTESTED_JKT = "attested_jkt";
    public static final String PLATFORM = "platform";
    public static final String MEASUREMENT = "measurement";
    public static final String POLICY_VERSION = "policy_version";
    public static final String APPRAISED_AT = "appraised_at";
    public static final String EXPIRES_AT = "expires_at";
    public static final String VERIFIER = "verifier";
    public static final String VERIFIER_RESULT_DIGEST = "verifier_result_digest";
    public static final String VERIFIER_RESULT = "verifier_result";
    public static final String KEY_BINDING_STRENGTH = "key_binding_strength";
    public static final String REASONS = "reasons";
    public static final String TRACE_RECORD_ID = "trace_record_id";
    public static final String REASON = "reason";

    public static final String ATTESTATION_ID_PREFIX = "att_";

    private AttestationConstants() {
    }
}
