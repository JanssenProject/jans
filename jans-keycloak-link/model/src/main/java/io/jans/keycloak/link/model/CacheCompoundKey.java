/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.model;

import java.io.Serializable;

/**
 * Compound key with String[] array
 * 
 * @author Yuriy Movchan Date: 07.21.2011
 */
public class CacheCompoundKey implements Serializable {

	private static final long serialVersionUID = -3366537601347036591L;

	private String primaryKeyValues;
	private String secondaryKeyValues;
	private String tertiaryKeyValues;

	public CacheCompoundKey(String primaryKeyValues, String secondaryKeyValues, String tertiaryKeyValues) {
		this.primaryKeyValues = primaryKeyValues;
		this.secondaryKeyValues = secondaryKeyValues;
		this.tertiaryKeyValues = tertiaryKeyValues;
	}

	public CacheCompoundKey(String[] keyValues) {
		if (keyValues.length > 0) {
			primaryKeyValues = keyValues[0];
		}
		if (keyValues.length > 1) {
			secondaryKeyValues = keyValues[1];
		}
		if (keyValues.length > 2) {
			tertiaryKeyValues = keyValues[2];
		}
	}

	public String getPrimaryKeyValues() {
		return primaryKeyValues;
	}

	public String getSecondaryKeyValues() {
		return secondaryKeyValues;
	}

	public String getTertiaryKeyValues() {
		return tertiaryKeyValues;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (null == primaryKeyValues ? 0 : primaryKeyValues.hashCode());
		result = prime * result + (null == secondaryKeyValues ? 0 : secondaryKeyValues.hashCode());
		result = prime * result + (null == tertiaryKeyValues? 0 : tertiaryKeyValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheCompoundKey other = (CacheCompoundKey) obj;
		if (null != primaryKeyValues && !primaryKeyValues.equalsIgnoreCase(other.primaryKeyValues))
			return false;
		if (null != secondaryKeyValues && !secondaryKeyValues.equalsIgnoreCase(other.secondaryKeyValues))
			return false;
		if (null != tertiaryKeyValues && !tertiaryKeyValues.equalsIgnoreCase(other.tertiaryKeyValues))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CacheCompoundKey [primaryKeyValues=").append(primaryKeyValues).append(", secondaryKeyValues=")
				.append(secondaryKeyValues).append(", tertiaryKeyValues=").append(tertiaryKeyValues)
				.append("]");
		return builder.toString();
	}

}
