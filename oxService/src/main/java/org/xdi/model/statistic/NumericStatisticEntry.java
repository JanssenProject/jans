/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model.statistic;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;

/**
 * Statistic entry which represents numeric value
 *
 * @author Yuriy Movchan Date: 07/27/2015
 */
public class NumericStatisticEntry extends StatisticEntry {

    @LdapJsonObject
    @LdapAttribute(name = "oxData")
	private NumericEventData eventData;

	public NumericStatisticEntry() {}

	public NumericStatisticEntry(String dn, String id, Date creationDate, NumericEventData eventData) {
		super(dn, id, creationDate);

		this.eventData = eventData;
	}

	public NumericEventData getEventData() {
		return eventData;
	}

	public void setEventData(NumericEventData eventData) {
		this.eventData = eventData;
	}

}
