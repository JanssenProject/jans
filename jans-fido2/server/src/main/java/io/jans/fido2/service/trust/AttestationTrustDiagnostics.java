/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.exception.Fido2TrustException;
import io.jans.fido2.model.trust.AttestationTrustDiagnostic;

/**
 * Recovers the trust diagnostic, if any, from the exception that failed a registration.
 * <p>
 * The code is read from a {@link Fido2TrustException} in the cause chain rather than matched against
 * the exception message. Message matching would silently stop classifying rejections the moment
 * someone rewords a log or error string, and the wording is not something the metrics contract should
 * depend on.
 * <p>
 * Registration failures that are not trust related resolve to {@code null}, and the caller keeps the
 * original message — nothing is lost for them.
 *
 * @author Janssen Project
 */
public final class AttestationTrustDiagnostics {

    /**
     * Guards against a self-referential or cyclic cause chain, which {@code initCause} does not
     * prevent. Deeper than any real FIDO2 attestation chain.
     */
    private static final int MAX_CAUSE_DEPTH = 16;

    private AttestationTrustDiagnostics() {
    }

    /**
     * Finds the trust failure behind a registration error.
     *
     * @param error the exception that failed the registration; may be null
     * @return the {@link Fido2TrustException} that describes the trust failure, or {@code null} when
     *         the failure was not trust related
     */
    public static Fido2TrustException find(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof Fido2TrustException) {
                return (Fido2TrustException) current;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return null;
    }

    /**
     * The diagnostic code to record for a registration failure.
     *
     * @param error the exception that failed the registration; may be null
     * @return the code name, or {@code null} when the failure was not trust related
     */
    public static String resolveCode(Throwable error) {
        Fido2TrustException trustFailure = find(error);
        if (trustFailure == null || trustFailure.getDiagnostic() == null) {
            return null;
        }
        return trustFailure.getDiagnostic().name();
    }

    /**
     * The AAGUID a trust rejection concerns.
     *
     * @param error the exception that failed the registration; may be null
     * @return the AAGUID, or {@code null} when the failure is not trust related or is not tied to a
     *         specific authenticator model
     */
    public static String resolveAaguid(Throwable error) {
        Fido2TrustException trustFailure = find(error);
        return trustFailure == null ? null : trustFailure.getAaguid();
    }

    /**
     * Convenience for the enum constant itself, for callers that need more than the name.
     */
    public static AttestationTrustDiagnostic resolve(Throwable error) {
        Fido2TrustException trustFailure = find(error);
        return trustFailure == null ? null : trustFailure.getDiagnostic();
    }
}
