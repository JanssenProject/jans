/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.ctap;

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
        ALGORITHM_MAPPINGS.put(-260, ED256);
        ALGORITHM_MAPPINGS.put(-261, ED512);
        ALGORITHM_MAPPINGS.put(-7, ES256);
        ALGORITHM_MAPPINGS.put(-36, ES384);
        ALGORITHM_MAPPINGS.put(-37, ES512);
        ALGORITHM_MAPPINGS.put(-25, ECDH_ES_HKDF_256);
    }

    private final int numericValue;

    CoseEC2Algorithm(int value) {
        this.numericValue = value;
    }

    public static CoseEC2Algorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    int getNumericValue() {
        return numericValue;
    }

}
