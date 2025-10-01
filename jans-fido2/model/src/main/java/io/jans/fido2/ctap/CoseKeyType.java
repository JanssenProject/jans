/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

public enum CoseKeyType {

    OKP(1), // https://tools.ietf.org/html/rfc8152#section-13
    EC2(2), // https://tools.ietf.org/html/rfc8152#section-13
    RSA(3); // https://tools.ietf.org/html/rfc8230#section-4

    private static final Map<Integer, CoseKeyType> KEY_MAPPINGS = new HashMap<>();

    static {
        KEY_MAPPINGS.put(1, OKP);
        KEY_MAPPINGS.put(2, EC2);
        KEY_MAPPINGS.put(3, RSA);
    }

    private final int numericValue;

    CoseKeyType(int numValue) {
        this.numericValue = numValue;
    }

    public static CoseKeyType fromNumericValue(int value) {
        return KEY_MAPPINGS.get(value);
    }

    int getNumericValue() {
        return numericValue;
    }

}
