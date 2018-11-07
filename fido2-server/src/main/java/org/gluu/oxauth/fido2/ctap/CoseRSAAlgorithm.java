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
        ALGORITHM_MAPPINGS.put(-65535, RS65535);
        ALGORITHM_MAPPINGS.put(-257, RS256);
        ALGORITHM_MAPPINGS.put(-258, RS384);
        ALGORITHM_MAPPINGS.put(-259, RS512);
        ALGORITHM_MAPPINGS.put(-262, RS1);
        ALGORITHM_MAPPINGS.put(-39, PS512);
        ALGORITHM_MAPPINGS.put(-38, PS384);
        ALGORITHM_MAPPINGS.put(-37, PS256);
    }

    private final int numericValue;

    CoseRSAAlgorithm(int value) {
        this.numericValue = value;
    }

    public static CoseRSAAlgorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    int getNumericValue() {
        return numericValue;
    }
}
