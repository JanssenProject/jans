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

package org.gluu.oxauth.fido2.model.entry;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

public enum Fido2RegistrationStatus implements LdapEnum {

    pending("pending", "Pending"), registered("registered", "Registered");

    private String value;
    private String displayName;

    private static Map<String, Fido2RegistrationStatus> mapByValues = new HashMap<String, Fido2RegistrationStatus>();

    static {
        for (Fido2RegistrationStatus enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private Fido2RegistrationStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Fido2RegistrationStatus getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends LdapEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
