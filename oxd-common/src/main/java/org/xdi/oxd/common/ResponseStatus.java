/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * Response status
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public enum ResponseStatus {
    OK("ok"),
    ERROR("error");

    /**
     * String value of status
     */
    private final String value;

    /**
     * Constructor
     *
     * @param p_value string value of status
     */
    private ResponseStatus(String p_value) {
        value = p_value;
    }

    /**
     * Returns string value of status
     *
     * @return string value of status
     */
    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Returns response status object based on string value of status.
     *
     * @param v string value of status
     * @return response status object based on string value of status
     */
    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ResponseStatus fromValue(String v) {
        if (StringUtils.isNotBlank(v)) {
            for (ResponseStatus t : values()) {
                if (t.getValue().equalsIgnoreCase(v)) {
                    return t;
                }
            }
        }
        return null;
    }
}
