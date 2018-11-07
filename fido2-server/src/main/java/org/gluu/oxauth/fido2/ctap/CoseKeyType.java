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
