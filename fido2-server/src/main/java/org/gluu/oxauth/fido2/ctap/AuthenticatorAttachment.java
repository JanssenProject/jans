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

public enum AuthenticatorAttachment {
	
	PLATFORM("platform"),
	CROSS_PLATFORM("cross-platform");

    private final String attachment;

    private static Map<String, AuthenticatorAttachment> KEY_MAPPINGS = new HashMap<String, AuthenticatorAttachment>();

    static {
        for (AuthenticatorAttachment enumType : values()) {
        	KEY_MAPPINGS.put(enumType.getAttachment(), enumType);
        }
    }

    AuthenticatorAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getAttachment() {
        return attachment;
    }

    public static AuthenticatorAttachment fromAttachmentValue(String attachment) {
        return KEY_MAPPINGS.get(attachment);
    }

}
