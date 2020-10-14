/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Val Pecaoco
 * Updated by jgomer on 2017-12-09.
 */
public enum ScimOperator {

    // eq | ne  | co   | sw  | ew  | gt           | lt           | ge | le
    // =  | !() | *{}* | {}* | *{} | (&(>=)(!{})) | (&(<=)(!{})) | >= | <=

    EQUAL ("eq"),
    NOT_EQUAL ("ne"),
    CONTAINS ("co"),
    STARTS_WITH ("sw"),
    ENDS_WITH ("ew"),
    GREATER_THAN ("gt"),
    LESS_THAN ("lt"),
    GREATER_THAN_OR_EQUAL ("ge"),
    LESS_THAN_OR_EQUAL ("le");

    private static Map<String, ScimOperator> mapByValues = new HashMap<>();

    private String value;

    static {
        for (ScimOperator operator : ScimOperator.values())
            mapByValues.put(operator.value, operator);
    }

    ScimOperator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ScimOperator getByValue(String value){
        return mapByValues.get(value);
    }

}
