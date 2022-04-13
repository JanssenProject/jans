/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.binding;

/**
 * <pre>
 * enum {
 *    provided_token_binding(0), referred_token_binding(1), (255)
 * } TokenBindingType;
 * </pre>
 *
 * @author Yuriy Zabrovarnyy
 */
public enum TokenBindingType {
    PROVIDED_TOKEN_BINDING("provided_token_binding", 0),
    REFERRED_TOKEN_BINDING("referred_token_binding", 1);

    private final String value;
    private final int byteValue;

    TokenBindingType(String value, int byteValue) {
        this.value = value;
        this.byteValue = byteValue;
    }

    public String getValue() {
        return value;
    }

    public int getByteValue() {
        return byteValue;
    }

    public static TokenBindingType valueOf(int byteValue) throws TokenBindingParseException{
        for (TokenBindingType v : values()) {
            if (v.getByteValue() == byteValue) {
                return v;
            }
        }
        throw new TokenBindingParseException("Failed to identify TokenBindingType, byteValue: " + byteValue);
    }

    @Override
    public String toString() {
        return "TokenBindingType{" +
                "value='" + value + '\'' +
                ", byteValue=" + byteValue +
                "} " + super.toString();
    }
}
