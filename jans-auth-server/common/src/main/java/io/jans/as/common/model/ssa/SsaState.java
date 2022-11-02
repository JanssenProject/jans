/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.ssa;

import io.jans.orm.annotation.AttributeEnum;

import java.util.Arrays;

public enum SsaState implements AttributeEnum {

    ACTIVE,
    EXPIRED,
    REVOKED,
    USED,
    ;

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String s) {
        return Arrays.stream(values()).filter(x -> x.name().equalsIgnoreCase(s)).findFirst().orElse(null);
    }
}
