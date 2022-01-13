/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

public enum CoseRSAAlgorithm {

    RS256(-257), // RSASSA-PKCS1-v1_5 w/ SHA-256 Section 8.2 of [RFC8017]
    RS65535(-65535), // RSASSA-PKCS1-v1_5 w/ SHA1
    RS384(-258), // RSASSA-PKCS1-v1_5 w/ SHA-384 Section 8.2 of [RFC8017]
    RS512(-259), // RSASSA-PKCS1-v1_5 w/ SHA-512 Section 8.2 of [RFC8017]
    RS1(-262), // RSASSA-PKCS1-v1_5 w/ SHA-1 Section 8.2 of [RFC8017]
    PS512(-39), // RSASSA-PSS w/ SHA-512 [RFC8230]
    PS384(-38), // RSASSA-PSS w/ SHA-384 [RFC8230]
    PS256(-37); // RSASSA-PSS w/ SHA-256 [RFC8230]

    private static final Map<Integer, CoseRSAAlgorithm> ALGORITHM_MAPPINGS = new HashMap<>();

    static {
        for (CoseRSAAlgorithm enumType : values()) {
        	ALGORITHM_MAPPINGS.put(enumType.getNumericValue(), enumType);
        }
    }

    private final int numericValue;

    CoseRSAAlgorithm(int value) {
        this.numericValue = value;
    }

    public static CoseRSAAlgorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    public int getNumericValue() {
        return numericValue;
    }

}
