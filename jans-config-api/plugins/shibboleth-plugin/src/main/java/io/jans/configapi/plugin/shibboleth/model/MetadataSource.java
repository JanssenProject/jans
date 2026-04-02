/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.model;

import java.util.HashMap;
import java.util.Map;
import io.jans.orm.annotation.AttributeEnum;

/**
 * Metadata source type
 * 
 */
public enum MetadataSource implements AttributeEnum {

    FILE("file", "File",1), URI("uri", "URI",2), UPSTREAM("upstream", "Upstream",3), MANUAL("manual", "Manual",4), MDQ("mdq", "MDQ",5);

    private final String value;
    private final String displayName;
    private final int rank; // used for ordering 

    private static final Map<String, MetadataSource> mapByValues = new HashMap<String, MetadataSource>();
    static {
        for (MetadataSource enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private MetadataSource(String value, String displayName,int rank) {
        this.value = value;
        this.displayName = displayName;
        this.rank = rank;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRank() {

        return this.rank;
    }

    public static MetadataSource getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }
    
    public static boolean contains(String name) {
        boolean result = false;
        for (MetadataSource direction : values()) {
            if (direction.name().equalsIgnoreCase(name)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
