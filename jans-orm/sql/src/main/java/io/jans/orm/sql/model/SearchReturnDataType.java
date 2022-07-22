/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Couchbase search return data type
 *
 * @author Yuriy Movchan Date: 12/16/2020
 */
public enum SearchReturnDataType implements AttributeEnum {

    SEARCH("search"),
    COUNT("count"),
    SEARCH_COUNT("search_count");    

    private String value;

    private static Map<String, SearchReturnDataType> MAP_BY_VALUES = new HashMap<String, SearchReturnDataType>();

    static {
        for (SearchReturnDataType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    SearchReturnDataType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static SearchReturnDataType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    @Override
    public SearchReturnDataType resolveByValue(String value) {
        return getByValue(value);
    }
}
