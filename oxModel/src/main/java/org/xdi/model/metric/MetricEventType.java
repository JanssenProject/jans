/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.metric;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.xdi.model.metric.ldap.MetricEntry;

/**
 * Metric event type declaration
 * 
 * @author Yuriy Movchan Date: 07/30/2015
 */
public interface MetricEventType extends LdapEnum {

	public String getValue();
	public String getDisplayName();
	public String getMetricName();

	public Enum<? extends LdapEnum> resolveByValue(String value);

	public Class<? extends MetricData> getEventDataType();

	public Class<? extends MetricEntry> getMetricEntryType();

}
