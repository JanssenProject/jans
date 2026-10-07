/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.metric;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregation period of pre-computed metric entries
 */
public enum MetricAggregationType implements AttributeEnum {

    HOURLY("HOURLY", "Hourly aggregation"),
    DAILY("DAILY", "Daily aggregation"),
    WEEKLY("WEEKLY", "Weekly aggregation"),
    MONTHLY("MONTHLY", "Monthly aggregation");

    private final String value;
    private final String displayName;

    private static final Map<String, MetricAggregationType> MAP_BY_VALUES = new HashMap<>();

    static {
        for (MetricAggregationType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    MetricAggregationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MetricAggregationType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
