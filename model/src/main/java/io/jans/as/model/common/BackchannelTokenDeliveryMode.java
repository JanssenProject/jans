/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public enum BackchannelTokenDeliveryMode implements HasParamName, AttributeEnum {

    POLL("poll"),
    PING("ping"),
    PUSH("push");

    private final String value;

    private static final Map<String, BackchannelTokenDeliveryMode> mapByValues = new HashMap<>();

    static {
        for (BackchannelTokenDeliveryMode enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    BackchannelTokenDeliveryMode (String value) {
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
     * Returns the corresponding {@link BackchannelTokenDeliveryMode} for a parameter backchannel_token_delivery_mode of
     * the access token requests.
     *
     * @param param The backchannel_token_delivery_mode parameter.
     * @return The corresponding Backchannel Token Delivery Mode if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static BackchannelTokenDeliveryMode fromString(String param) {
        if (param != null) {
            for (BackchannelTokenDeliveryMode deliveryMode : BackchannelTokenDeliveryMode.values()) {
                if (param.equals(deliveryMode.value)) {
                    return deliveryMode;
                }
            }
        }

        return null;
    }

    public static BackchannelTokenDeliveryMode getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the backchannel_token_delivery_mode parameter.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}