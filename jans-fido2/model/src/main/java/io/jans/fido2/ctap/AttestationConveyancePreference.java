/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

public enum AttestationConveyancePreference {

    direct("direct"),
    indirect("indirect"),
    enterprise("enterprise"),
    none("none");

    private String keyName;

    private AttestationConveyancePreference(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return this.keyName;
    }

}
