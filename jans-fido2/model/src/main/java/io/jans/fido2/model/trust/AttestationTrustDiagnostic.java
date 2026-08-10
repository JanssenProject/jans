/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    /** Shared prefix for internal diagnostic codes, of which these are the attestation-trust subset. */
    public static final String CODE_PREFIX = "JFS_";

    private static final Set<String> CODE_NAMES;

    static {
        Set<String> names = new HashSet<>();
        for (AttestationTrustDiagnostic diagnostic : values()) {
            names.add(diagnostic.name());
        }
        CODE_NAMES = Collections.unmodifiableSet(names);
    }

    /**
     * Whether a recorded {@code errorReason} is one of <em>these</em> diagnostic codes.
     * <p>
     * Deliberately matched against the enum values rather than the shared {@link #CODE_PREFIX}: the
     * native-metrics work writes its own {@code JFS_} codes into the same {@code errorReason} field,
     * and a prefix test would file those under {@link #CATEGORY} too — quietly inflating the
     * attestation-rejection analytics with rejections that have nothing to do with attestation trust.
     *
     * @param errorReason the recorded reason; may be null
     * @return true when the reason is an attestation-trust diagnostic code
     */
    public static boolean isDiagnosticCode(String errorReason) {
        return errorReason != null && CODE_NAMES.contains(errorReason);
    }
}
