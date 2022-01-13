/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

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
