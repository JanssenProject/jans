/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;


public enum FilterOperator {

    EQUALITY("="), LESS("<"), LESS_OR_EQUAL("<="), GREATER(">"), GREATER_OR_EQUAL(">=");

    private String sign;

    FilterOperator(String sign) {
        this.sign = sign;
    }

    public final String getSign() {
        return sign;
    }

}
