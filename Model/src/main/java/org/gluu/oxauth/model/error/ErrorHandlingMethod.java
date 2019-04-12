/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.error;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.HasParamName;
import org.gluu.persist.annotation.AttributeEnum;

/**
 * This class define error handling methods
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 12/07/2018
 */
public enum ErrorHandlingMethod implements HasParamName, AttributeEnum {

    INTERNAL("internal"),

    REMOTE("remote");

    private final String value;

    private static Map<String, ErrorHandlingMethod> mapByValues = new HashMap<String, ErrorHandlingMethod>();

    static {
        for (ErrorHandlingMethod enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private ErrorHandlingMethod() {
        this.value = null;
    }

    private ErrorHandlingMethod(String value) {
        this.value = value;
    }

    /**
     * Gets param name.
     *
     * @return param name
     */
    public String getParamName() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Returns the corresponding {@link GrantType} for a parameter grant_type of
     * the access token requests. For the extension grant type, the parameter
     * should be a valid URI.
     *
     * @param param The grant_type parameter.
     * @return The corresponding grant type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static ErrorHandlingMethod fromString(String param) {
        if (param != null) {
            for (ErrorHandlingMethod hm : ErrorHandlingMethod.values()) {
                if (param.equals(hm.value)) {
                    return hm;
                }
            }
        }

        return null;
    }

    public static String[] toStringArray(ErrorHandlingMethod[] grantTypes) {
        if (grantTypes == null) {
            return null;
        }

        String[] resultGrantTypes = new String[grantTypes.length];
        for (int i = 0; i < grantTypes.length; i++) {
            resultGrantTypes[i] = grantTypes[i].getValue();
        }

        return resultGrantTypes;
    }

    public static ErrorHandlingMethod getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the grant_type parameter.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
