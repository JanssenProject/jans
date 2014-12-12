/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import com.unboundid.util.StaticUtils;

/**
 * Ldap custom attribute
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class LdapCustomAttribute implements Serializable {

	private static final long serialVersionUID = 1468440094325406153L;

	private String propertyName;
	private String[] values;

	public LdapCustomAttribute() {
	}

	public LdapCustomAttribute(String propertyName, String value) {
		this.propertyName = propertyName;
		setValue(value);
	}

	public LdapCustomAttribute(String propertyName, String[] values) {
		this.propertyName = propertyName;
		this.values = values;
	}

	public String getValue() {
		if (this.values == null) {
			return null;
		}

		if (this.values.length > 0) {
			return this.values[0];
		}

		return null;
	}

	public void setValue(String value) {
		if (this.values == null) {
			this.values = new String[0];
		}

		if (this.values.length != 1) {
			this.values = new String[1];
		}
		this.values[0] = value;
	}

	public Date getDate() {
		if (this.values == null) {
			return null;
		}

		if (this.values.length > 0 && values[0] != null) {
			try {
				return StaticUtils.decodeGeneralizedTime((String) values[0]);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public void setDate(Date date) {
		if (this.values == null) {
			this.values = new String[0];
		}

		if (this.values.length != 1) {
			this.values = new String[1];
		}
		this.values[0] = StaticUtils.encodeGeneralizedTime(date);
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
	}

	public final String getPropertyName() {
		return propertyName;
	}

	public final void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getDisplayValue() {
		if (values == null) {
			return "";
		}

		if (values.length == 1) {
			return values[0];
		}

		StringBuilder sb = new StringBuilder(values[0]);
		for (int i = 1; i < values.length; i++) {
			sb.append(", ").append(values[i]);
		}

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}

		LdapCustomAttribute other = (LdapCustomAttribute) obj;
		if (propertyName == null) {
			if (other.propertyName != null) {
				return false;
			}
		} else if (!propertyName.equals(other.propertyName)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format("Attribute [propertyName=%s, values=%s]", propertyName, Arrays.toString(values));
	}

}
