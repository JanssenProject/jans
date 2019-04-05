package org.gluu.persist.couchbase.model;
/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Couchbase search return data type
 *
 * @author Yuriy Movchan Date: 05/04/2019
 */
public enum SearchReturnDataType implements LdapEnum {

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
