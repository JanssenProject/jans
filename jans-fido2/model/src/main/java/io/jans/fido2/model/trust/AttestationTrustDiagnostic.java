/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Internal diagnostic codes for attestation rejections caused by trust or metadata problems.
 * <p>
 * These are written into the existing {@code Fido2MetricsEntry.errorReason} field, with
 * {@link #CATEGORY} as the error category, so an operator can tell an unknown AAGUID from an untrusted
 * root instead of reading free-text exception messages. They are internal: the public FIDO response
 * envelope is unchanged and these codes never reach the client body.
 * <p>
 * Each constant carries the message markers of the failure site it maps to, so the mapping stays
 * traceable to the code that raises it.
 *
 * @author Janssen Project
 */
public enum AttestationTrustDiagnostic {

    /** {@code MdsService}: "Authenticator not in TOC aaguid …". */
    JFS_AAGUID_NOT_IN_MDS("not in toc"),

    /**
     * {@code MdsService}: "Authenticator … status undesirable …" and
     * {@code CertificateVerifier.verifyStatusAcceptable}: "… due to status: …".
     */
    JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE("status undesirable", "due to status:"),

    /** {@code CertificateVerifier.verifyAttestationCertificates}: "Problem with certificate …". */
    JFS_ROOT_CERT_NOT_TRUSTED("problem with certificate"),

    /** The attestation statement format is not one the server accepts. */
    JFS_ATTESTATION_FORMAT_NOT_PERMITTED("unsupported attestation", "attestation format"),

    /** Metadata was required to validate the authenticator but could not be fetched or loaded. */
    JFS_MDS_UNAVAILABLE("mds unavailable", "metadata service");

    /** Value written to {@code Fido2MetricsEntry.errorCategory} for every code in this enum. */
    public static final String CATEGORY = "ATTESTATION_TRUST";

    /** Shared prefix, used to recognise a diagnostic code without matching the enum itself. */
    public static final String CODE_PREFIX = "JFS_";

    private final List<String> messageMarkers;

    AttestationTrustDiagnostic(String... messageMarkers) {
        this.messageMarkers = Collections.unmodifiableList(Arrays.asList(messageMarkers));
    }

    public List<String> getMessageMarkers() {
        return messageMarkers;
    }

    /**
     * Classify a failure message as a trust diagnostic.
     *
     * @param errorMessage the message of the exception that failed the registration; may be null
     * @return the matching code name, or {@code null} when the failure is not trust related — in which
     *         case the caller keeps the original message, so nothing is lost for non-trust failures
     */
    public static String resolveCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return null;
        }
        String normalized = errorMessage.toLowerCase();
        for (AttestationTrustDiagnostic diagnostic : values()) {
            for (String marker : diagnostic.messageMarkers) {
                if (normalized.contains(marker)) {
                    return diagnostic.name();
                }
            }
        }
        return null;
    }

    /**
     * Whether a recorded {@code errorReason} is one of these diagnostic codes.
     */
    public static boolean isDiagnosticCode(String errorReason) {
        return errorReason != null && errorReason.startsWith(CODE_PREFIX);
    }
}
