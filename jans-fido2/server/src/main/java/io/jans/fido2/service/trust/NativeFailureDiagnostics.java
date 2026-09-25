/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.exception.Fido2NativeFailureException;
import io.jans.fido2.model.trust.NativeFailureDiagnostic;

/**
 * Recovers the native-failure diagnostic, if any, from the exception that failed a registration or
 * authentication ceremony. Mirrors {@link AttestationTrustDiagnostics} for the same reasons: the code
 * is read from a {@link Fido2NativeFailureException} in the cause chain rather than matched against
 * the exception message, so rewording a log string never silently stops classifying failures.
 * <p>
 * Failures that are not one of these diagnostics resolve to {@code null}, and the caller keeps the
 * original message — nothing is lost for them.
 *
 * @author Janssen Project
 */
public final class NativeFailureDiagnostics {

    /**
     * Guards against a self-referential or cyclic cause chain, which {@code initCause} does not
     * prevent. Deeper than any real FIDO2 verification chain.
     */
    private static final int MAX_CAUSE_DEPTH = 16;

    private NativeFailureDiagnostics() {
    }

    /**
     * Finds the native failure behind a ceremony error.
     *
     * @param error the exception that failed the ceremony; may be null
     * @return the {@link Fido2NativeFailureException} that describes the failure, or {@code null}
     *         when the failure was not one of these diagnostics
     */
    public static Fido2NativeFailureException find(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof Fido2NativeFailureException) {
                return (Fido2NativeFailureException) current;
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
     * The diagnostic code to record for a ceremony failure.
     *
     * @param error the exception that failed the ceremony; may be null
     * @return the code name, or {@code null} when the failure was not one of these diagnostics
     */
    public static String resolveCode(Throwable error) {
        Fido2NativeFailureException failure = find(error);
        if (failure == null || failure.getDiagnostic() == null) {
            return null;
        }
        return failure.getDiagnostic().name();
    }

    /**
     * Convenience for the enum constant itself, for callers that need more than the name.
     */
    public static NativeFailureDiagnostic resolve(Throwable error) {
        Fido2NativeFailureException failure = find(error);
        return failure == null ? null : failure.getDiagnostic();
    }
}
