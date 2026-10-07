/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception;

import io.jans.fido2.model.trust.AttestationTrustDiagnostic;

/**
 * A {@link Fido2RuntimeException} that also carries <em>why</em> the attestation was not trusted.
 * <p>
 * It is a subclass rather than a new exception type on purpose: every existing {@code catch
 * (Fido2RuntimeException)} continues to catch it, and the status and message it is constructed with
 * are the same ones the plain exception carried. Nothing about the rejection changes — the diagnostic
 * code simply travels with the failure so metrics can record it instead of a free-text message.
 * <p>
 * Trust failures that must surface as a FIDO error envelope throw a {@code WebApplicationException}
 * instead; those sites pass an instance of this class as the <em>cause</em>, which is where the
 * diagnostic resolver finds it.
 *
 * @author Janssen Project
 */
public class Fido2TrustException extends Fido2RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient AttestationTrustDiagnostic diagnostic;

    private final String aaguid;

    public Fido2TrustException(AttestationTrustDiagnostic diagnostic, String errorMessage) {
        this(diagnostic, null, errorMessage);
    }

    public Fido2TrustException(AttestationTrustDiagnostic diagnostic, String aaguid, String errorMessage) {
        super(errorMessage);
        this.diagnostic = diagnostic;
        this.aaguid = aaguid;
    }

    public Fido2TrustException(AttestationTrustDiagnostic diagnostic, String aaguid, String errorMessage,
            Throwable cause) {
        super(errorMessage, cause);
        this.diagnostic = diagnostic;
        this.aaguid = aaguid;
    }

    /** The reason the authenticator was not trusted. */
    public AttestationTrustDiagnostic getDiagnostic() {
        return diagnostic;
    }

    /**
     * The AAGUID the rejection concerns, or {@code null} when the failure is not tied to one (for
     * example an attestation format that the mode does not permit).
     */
    public String getAaguid() {
        return aaguid;
    }
}
