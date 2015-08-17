/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.metric.ldap;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.model.metric.MetricData;
import org.xdi.model.metric.MetricType;
import org.xdi.model.metric.counter.CounterMetricData;
import org.xdi.model.metric.counter.CounterMetricEntry;
import org.xdi.model.metric.ldap.MetricEntry;
import org.xdi.model.metric.timer.TimerMetricData;
import org.xdi.model.metric.timer.TimerMetricEntry;

/**
 * oxAuth metric event type
 * 
 * @author Yuriy Movchan Date: 07/30/2015
 */
public enum OxAuthMetricType implements MetricType {

		OXAUTH_USER_AUTHENTICATION_SUCCESS("user_authentication_success",  "Count successfull oxAuth user authentications", CounterMetricData.class, CounterMetricEntry.class),
		OXAUTH_USER_AUTHENTICATION_FAILURES("user_authentication_failure", "Count failed oxAuth user authentications", CounterMetricData.class, CounterMetricEntry.class),
		OXAUTH_USER_AUTHENTICATION_RATE("user_authentication_rate", "User authentication rate", TimerMetricData.class, TimerMetricEntry.class),
		DYNAMIC_CLIENT_REGISTRATION_RATE("dynamic_client_registration_rate", "Dynamic client registration rate", TimerMetricData.class, TimerMetricEntry.class);

		private String value;
		private String displayName;
		private Class<? extends MetricData> eventDataType;
		private Class<? extends MetricEntry> metricEntryType;

		private static Map<String, OxAuthMetricType> mapByValues = new HashMap<String, OxAuthMetricType>();

		static {
			for (OxAuthMetricType enumType : values()) {
				mapByValues.put(enumType.getValue(), enumType);
			}
		}

		private OxAuthMetricType(String value, String displayName, Class<? extends MetricData> eventDataType, Class<? extends MetricEntry> metricEntryType) {
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

		public static OxAuthMetricType getByValue(String value) {
			return mapByValues.get(value);
		}

		public Enum<? extends LdapEnum> resolveByValue(String value) {
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
