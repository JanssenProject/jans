/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.model;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.io.Serializable;

/**
 * GluuInumMap
 * 
 * @author Yuriy Movchan Date: 07.13.2011
 */
@DataEntry(sortBy = { "inum" })
@ObjectClass(value = "jansInumMap")
public class JansInumMap extends Entry implements Serializable {

	private static final long serialVersionUID = -2190480357430436503L;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName(name = "jansPrimaryKeyAttrName")
	private String primaryKeyAttrName;

	@AttributeName(name = "jansPrimaryKeyValue")
	private String primaryKeyValues;

	@AttributeName(name = "jansSecondaryKeyAttrName")
	private String secondaryKeyAttrName;

	@AttributeName(name = "jansSecondaryKeyValue")
	private String secondaryKeyValues;

	@AttributeName(name = "jansTertiaryKeyAttrName")
	private String tertiaryKeyAttrName;

	@AttributeName(name = "tertiaryKeyValue")
	private String tertiaryKeyValues;

	@AttributeName(name = "jansStatus")
	private GluuStatus status;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getPrimaryKeyAttrName() {
		return primaryKeyAttrName;
	}

	public void setPrimaryKeyAttrName(String primaryKeyAttrName) {
		this.primaryKeyAttrName = primaryKeyAttrName;
	}

	public String getPrimaryKeyValues() {
		return primaryKeyValues;
	}

	public void setPrimaryKeyValues(String primaryKeyValues) {
		this.primaryKeyValues = primaryKeyValues;
	}

	public String getSecondaryKeyAttrName() {
		return secondaryKeyAttrName;
	}

	public void setSecondaryKeyAttrName(String secondaryKeyAttrName) {
		this.secondaryKeyAttrName = secondaryKeyAttrName;
	}

	public String getSecondaryKeyValues() {
		return secondaryKeyValues;
	}

	public void setSecondaryKeyValues(String secondaryKeyValues) {
		this.secondaryKeyValues = secondaryKeyValues;
	}

	public String getTertiaryKeyAttrName() {
		return tertiaryKeyAttrName;
	}

	public void setTertiaryKeyAttrName(String tertiaryKeyAttrName) {
		this.tertiaryKeyAttrName = tertiaryKeyAttrName;
	}

	public String getTertiaryKeyValues() {
		return tertiaryKeyValues;
	}

	public void setTertiaryKeyValues(String tertiaryKeyValues) {
		this.tertiaryKeyValues = tertiaryKeyValues;
	}

	public GluuStatus getStatus() {
		return status;
	}

	public void setStatus(GluuStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GluuInumMap [inum=").append(inum).append(", primaryKeyAttrName=").append(primaryKeyAttrName)
				.append(", primaryKeyValues=").append(primaryKeyValues).append(", secondaryKeyAttrName=")
				.append(secondaryKeyAttrName).append(", secondaryKeyValues=").append(secondaryKeyValues)
				.append(", tertiaryKeyAttrName=").append(tertiaryKeyAttrName).append(", tertiaryKeyValues=")
				.append(tertiaryKeyValues).append(", status=").append(status).append("]");
		return builder.toString();
	}
}
