/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

// S115: the constant names intentionally mirror the WebAuthn attestation "fmt" identifiers
// (with '-' replaced by '_') because AttestationProcessorFactory resolves them via
// AttestationFormat.valueOf(fmt.replace('-', '_')). Renaming them to UPPER_CASE would break that lookup.
@SuppressWarnings("java:S115")
public enum AttestationFormat {

    fido_u2f("fido-u2f"), packed("packed"), tpm("tpm"), none("none"), apple("apple"), android_key("android-key");

    private final String fmt;

    AttestationFormat(String fmt) {
        this.fmt = fmt;
    }

    public String getFmt() {
        return fmt;
    }

}
