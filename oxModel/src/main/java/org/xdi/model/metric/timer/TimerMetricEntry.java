/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.metric.timer;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.xdi.model.metric.ldap.MetricEntry;

/**
 * Timer entry which represents timer value
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class TimerMetricEntry extends MetricEntry {

    @LdapJsonObject
    @LdapAttribute(name = "oxData")
	private TimerMetricData eventData;

	public TimerMetricEntry() {}

	public TimerMetricEntry(String dn, String id, Date creationDate, TimerMetricData eventData) {
		super(dn, id, creationDate);

		this.eventData = eventData;
	}

	public TimerMetricData getEventData() {
		return eventData;
	}

	public void setEventData(TimerMetricData eventData) {
		this.eventData = eventData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimerMetricEntry [eventData=").append(eventData).append(", toString()=").append(super.toString()).append("]");
		return builder.toString();
	}

}
