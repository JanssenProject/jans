/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.statistic;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Attribute Usage Type
 * 
 * @author Yuriy Movchan Date: 02/12/2014
 */
public enum StatisticEventType implements LdapEnum {

	OXAUTH_USER_AUTHENTICATION_SUCCESS("oxauth_user_authentication_success",  "Count successfull oxAuth user authentications", NumericEventData.class, NumericStatisticEntry.class),
	OXAUTH_USER_AUTHENTICATION_FAILURES("oxauth_user_authentication_failure", "Count failed oxAuth user authentications", NumericEventData.class, NumericStatisticEntry.class);

	private String value;
	private String displayName;
	private Class<? extends EventData> eventDataType;
	private Class<? extends StatisticEntry> statisticEntryType;

	private static Map<String, StatisticEventType> mapByValues = new HashMap<String, StatisticEventType>();

	static {
		for (StatisticEventType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private StatisticEventType(String value, String displayName, Class<? extends EventData> eventDataType, Class<? extends StatisticEntry> statisticEntryType) {
		this.value = value;
		this.displayName = displayName;
		this.eventDataType = eventDataType;
		this.statisticEntryType = statisticEntryType;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static StatisticEventType getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends LdapEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	public Class<? extends EventData> getEventDataType() {
		return eventDataType;
	}

	public Class<? extends StatisticEntry> getStatisticEntryType() {
		return statisticEntryType;
	}

	@Override
	public String toString() {
		return value;
	}

}
