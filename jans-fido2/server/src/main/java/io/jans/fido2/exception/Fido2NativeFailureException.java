/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception;

import io.jans.fido2.model.trust.NativeFailureDiagnostic;

/**
 * A {@link Fido2RuntimeException} that also carries <em>why</em> a ceremony failed, for failure modes
 * that are not attestation-trust related (see {@link Fido2TrustException} for those). It is a
 * subclass rather than a new exception type for the same reason {@code Fido2TrustException} is: every
 * existing {@code catch (Fido2RuntimeException)} continues to catch it unchanged, and the message it
 * is constructed with is the same one the plain exception carried — the diagnostic code simply
 * travels with the failure so metrics can record it instead of a free-text message.
 *
 * @author Janssen Project
 */
public class Fido2NativeFailureException extends Fido2RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient NativeFailureDiagnostic diagnostic;

    public Fido2NativeFailureException(NativeFailureDiagnostic diagnostic, String errorMessage) {
        super(errorMessage);
        this.diagnostic = diagnostic;
    }

    /** The reason the ceremony failed. */
    public NativeFailureDiagnostic getDiagnostic() {
        return diagnostic;
    }
}
