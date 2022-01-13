/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

public enum CoseEC2Algorithm {

    ED256(-260), // TPM_ECC_BN_P256 curve w/ SHA-256
    ED512(-261), // ECC_BN_ISOP512 curve w/ SHA-512
    ES256(-7), // ECDSA w/ SHA-256
    ES384(-36), // ECDSA w/ SHA-384
    ES512(-37), // ECDSA w/ SHA-512
    ECDH_ES_HKDF_256(-25);

    private static final Map<Integer, CoseEC2Algorithm> ALGORITHM_MAPPINGS = new HashMap<>();

    static {
        for (CoseEC2Algorithm enumType : values()) {
        	ALGORITHM_MAPPINGS.put(enumType.getNumericValue(), enumType);
        }
    }

    private final int numericValue;

    CoseEC2Algorithm(int value) {
        this.numericValue = value;
    }

    public static CoseEC2Algorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    public int getNumericValue() {
        return numericValue;
    }

}
