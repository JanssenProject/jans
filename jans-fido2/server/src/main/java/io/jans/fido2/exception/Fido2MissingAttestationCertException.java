/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception;

/**
 * Missing attestation certificate Exception
 *
 */
public class Fido2MissingAttestationCertException extends Fido2RuntimeException {

	private static final long serialVersionUID = 9114154955909766262L;

    public Fido2MissingAttestationCertException(String errorMessage) {
        super(errorMessage);
    }

    public Fido2MissingAttestationCertException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

}
