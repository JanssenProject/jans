package io.jans.orm.sql.model;

import java.io.Serializable;
import java.util.Arrays;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
* GluuInumMap
* 
* @author Yuriy Movchan Date: 04/20/2022
*/
@DataEntry(sortBy = { "inum" })
@ObjectClass(value = "jansInumMap")
public class SimleInumMap extends Entry implements Serializable {

	private static final long serialVersionUID = -2190480357430436503L;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName
	private String primaryKeyAttrName;

	@AttributeName(name = "primaryKeyValue")
	private String[] primaryKeyValues;

	@AttributeName
	private String secondaryKeyAttrName;

	@AttributeName(name = "secondaryKeyValue")
	private String[] secondaryKeyValues;

	@AttributeName
	private String tertiaryKeyAttrName;

	@AttributeName(name = "tertiaryKeyValue")
	private String[] tertiaryKeyValues;

	@AttributeName(name = "jansStatus")
	private Status status;

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

	public String[] getPrimaryKeyValues() {
		return primaryKeyValues;
	}

	public void setPrimaryKeyValues(String[] primaryKeyValues) {
		this.primaryKeyValues = primaryKeyValues;
	}

	public String getSecondaryKeyAttrName() {
		return secondaryKeyAttrName;
	}

	public void setSecondaryKeyAttrName(String secondaryKeyAttrName) {
		this.secondaryKeyAttrName = secondaryKeyAttrName;
	}

	public String[] getSecondaryKeyValues() {
		return secondaryKeyValues;
	}

	public void setSecondaryKeyValues(String[] secondaryKeyValues) {
		this.secondaryKeyValues = secondaryKeyValues;
	}

	public String getTertiaryKeyAttrName() {
		return tertiaryKeyAttrName;
	}

	public void setTertiaryKeyAttrName(String tertiaryKeyAttrName) {
		this.tertiaryKeyAttrName = tertiaryKeyAttrName;
	}

	public String[] getTertiaryKeyValues() {
		return tertiaryKeyValues;
	}

	public void setTertiaryKeyValues(String[] tertiaryKeyValues) {
		this.tertiaryKeyValues = tertiaryKeyValues;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GluuInumMap [inum=").append(inum).append(", primaryKeyAttrName=").append(primaryKeyAttrName)
				.append(", primaryKeyValues=").append(Arrays.toString(primaryKeyValues)).append(", secondaryKeyAttrName=")
				.append(secondaryKeyAttrName).append(", secondaryKeyValues=").append(Arrays.toString(secondaryKeyValues))
				.append(", tertiaryKeyAttrName=").append(tertiaryKeyAttrName).append(", tertiaryKeyValues=")
				.append(Arrays.toString(tertiaryKeyValues)).append(", status=").append(status).append("]");
		return builder.toString();
	}

}
