/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2020 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.fido2.ctap;

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
