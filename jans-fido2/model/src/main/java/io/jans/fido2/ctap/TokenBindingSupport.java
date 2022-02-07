/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Movchan
 * @version March 9, 2020
 */
public enum TokenBindingSupport {
	
	SUPPORTED("supported"),
	NOT_SUPPORTED("not-supported");

    private final String status;

    private static Map<String, TokenBindingSupport> KEY_MAPPINGS = new HashMap<>();

    static {
        for (TokenBindingSupport enumType : values()) {
        	KEY_MAPPINGS.put(enumType.getStatus(), enumType);
        }
    }

    TokenBindingSupport(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static TokenBindingSupport fromStatusValue(String attachment) {
        return KEY_MAPPINGS.get(attachment);
    }

}
