/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

/**
 * Internal diagnostic codes for attestation rejections caused by a trust or metadata problem.
 * <p>
 * Without these, an unknown AAGUID, an authenticator blocked by an MDS status report and an untrusted
 * root certificate are indistinguishable from each other — and from any other registration failure —
 * once they reach the metrics store as free-text exception messages.
 * <p>
 * A code is written to {@code Fido2MetricsEntry.errorReason}, with {@link #CATEGORY} as the error
 * category, so rejections can be counted by cause. These are strictly internal: the public FIDO
 * response envelope is unchanged and a code never reaches the client body.
 *
 * @author Janssen Project
 */
public enum AttestationTrustDiagnostic {

    /** The authenticator's AAGUID is not present in the loaded TOC entries. */
    JFS_AAGUID_NOT_IN_MDS,

    /** The loaded TOC blob was past its {@code nextUpdate} when the authenticator was validated. */
    JFS_MDS_METADATA_EXPIRED,

    /** Metadata was required by the effective attestation mode, but none could be obtained. */
    JFS_MDS_UNAVAILABLE,

    /** The attestation statement format is not one the effective mode permits. */
    JFS_ATTESTATION_FORMAT_NOT_PERMITTED,

    /** The attestation certificate chain did not verify to a trusted root. */
    JFS_ROOT_CERT_NOT_TRUSTED,

    /** Apple attestation was attempted while the Apple WebAuthn root CA is absent. */
    JFS_APPLE_ROOT_CA_MISSING,

    /** The authenticator is blocked by an MDS status report (revoked, compromised, and so on). */
    JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE;

    /** Value written to {@code Fido2MetricsEntry.errorCategory} for every code in this enum. */
    public static final String CATEGORY = "ATTESTATION_TRUST";

    /** Shared prefix, so a recorded reason can be recognised as a code without matching the enum. */
    public static final String CODE_PREFIX = "JFS_";

    /**
     * Whether a recorded {@code errorReason} is one of these diagnostic codes.
     * <p>
     * Matching on the prefix rather than the enum values keeps this true for codes added by the
     * related native-metrics work, which writes into the same field.
     *
     * @param errorReason the recorded reason; may be null
     * @return true when the reason is a diagnostic code rather than a free-text message
     */
    public static boolean isDiagnosticCode(String errorReason) {
        return errorReason != null && errorReason.startsWith(CODE_PREFIX);
    }
}
