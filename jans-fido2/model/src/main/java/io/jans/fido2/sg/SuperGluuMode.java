/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.sg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Movchan
 * @version January 24, 2023
 */
public enum SuperGluuMode {
	
	ONE_STEP("one_step"),
	TWO_STEP("two_step");

    private final String mode;

    private static Map<String, SuperGluuMode> KEY_MAPPINGS = new HashMap<>();

    static {
        for (SuperGluuMode enumType : values()) {
        	KEY_MAPPINGS.put(enumType.getMode(), enumType);
        }
    }

    SuperGluuMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public static SuperGluuMode fromModeValue(String attachment) {
        return KEY_MAPPINGS.get(attachment);
    }

}
