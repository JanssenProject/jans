/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.search.filter;

/**
 * Filter operation types
 *
 * @author Yuriy Movchan Date: 2017/12/13
 */
public enum FilterType {

    RAW(""), PRESENCE("*"), EQUALITY("="), LESS_OR_EQUAL("<="), GREATER_OR_EQUAL(">="), APPROXIMATE_MATCH("~"), SUBSTRING("="), NOT("!"),
    OR("|"), AND("&"), LOWERCASE("lowercase");

    private String sign;

    FilterType(String sign) {
        this.sign = sign;
    }

    public final String getSign() {
        return sign;
    }

}
