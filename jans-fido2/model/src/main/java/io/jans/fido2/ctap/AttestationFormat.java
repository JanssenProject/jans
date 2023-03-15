/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

public enum AttestationFormat {

    fido_u2f("fido-u2f"), packed("packed"), tpm("tpm"), android_key("android-key"), android_safetynet("android-safetynet"), none("none"), apple("apple"),
    fido_u2f_super_gluu("fido-u2f-super-gluu");

    private final String fmt;

    AttestationFormat(String fmt) {
        this.fmt = fmt;
    }

    public String getFmt() {
        return fmt;
    }

}
