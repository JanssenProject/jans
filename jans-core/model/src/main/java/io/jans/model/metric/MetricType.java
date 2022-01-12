/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.metric;

import io.jans.model.metric.counter.CounterMetricData;
import io.jans.model.metric.counter.CounterMetricEntry;
import io.jans.model.metric.ldap.MetricEntry;
import io.jans.model.metric.timer.TimerMetricData;
import io.jans.model.metric.timer.TimerMetricEntry;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric event type declaration
 *
 * @author Yuriy Movchan Date: 07/30/2015
 */
public enum MetricType implements AttributeEnum {

    USER_AUTHENTICATION_SUCCESS("user_authentication_success",
            "Count successfull user authentications", CounterMetricData.class, CounterMetricEntry.class),
    USER_AUTHENTICATION_FAILURES("user_authentication_failure",
            "Count failed user authentications", CounterMetricData.class, CounterMetricEntry.class),
    USER_AUTHENTICATION_RATE("user_authentication_rate",
            "User authentication rate", TimerMetricData.class, TimerMetricEntry.class),
    DYNAMIC_CLIENT_REGISTRATION_RATE("dynamic_client_registration_rate",
            "Dynamic client registration rate", TimerMetricData.class, TimerMetricEntry.class),

	TOKEN_AUTHORIZATION_CODE_COUNT("tkn_authorization_code_count",
            "Count successfull issued authorization code tokens", CounterMetricData.class, CounterMetricEntry.class),
	TOKEN_ACCESS_TOKEN_COUNT("tkn_access_token_count",
            "Count successfull issued access tokens", CounterMetricData.class, CounterMetricEntry.class),
	TOKEN_ID_TOKEN_COUNT("tkn_id_token_count",
            "Count successfull issued id tokens", CounterMetricData.class, CounterMetricEntry.class),
	TOKEN_REFRESH_TOKEN_COUNT("tkn_refresh_token_count",
            "Count successfull issued refresh tokens", CounterMetricData.class, CounterMetricEntry.class),
	TOKEN_LONG_LIVED_ACCESS_TOKEN_COUNT("tkn_long_lived_access_token_count",
            "Count successfull issued long lived access tokens", CounterMetricData.class, CounterMetricEntry.class);

    private String value;
    private String displayName;
    private Class<? extends MetricData> eventDataType;
    private Class<? extends MetricEntry> metricEntryType;

    private static Map<String, MetricType> MAP_BY_VALUES = new HashMap<String, MetricType>();

    static {
        for (MetricType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    MetricType(String value, String displayName, Class<? extends MetricData> eventDataType, Class<? extends MetricEntry> metricEntryType) {
        this.value = value;
        this.displayName = displayName;
        this.eventDataType = eventDataType;
        this.metricEntryType = metricEntryType;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMetricName() {
        return value;
    }

    public static MetricType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    public Class<? extends MetricData> getEventDataType() {
        return eventDataType;
    }

    public Class<? extends MetricEntry> getMetricEntryType() {
        return metricEntryType;
    }

    @Override
    public String toString() {
        return value;
    }

}
