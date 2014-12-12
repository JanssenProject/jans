/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.ldap.model;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import com.unboundid.util.StaticUtils;

/**
 * Custom attribute
 * 
 * @author Yuriy Movchan Date: 04/08/2014
 */
public class CustomAttribute implements Serializable, Comparable<CustomAttribute> {

	private static final long serialVersionUID = 1468440094325406153L;

	private String name;
	private String[] values;

	public CustomAttribute() {
	}

	public CustomAttribute(String name, String value) {
		this.name = name;
		setValue(value);
	}

	public CustomAttribute(String name, Date value) {
		this.name = name;
		setDate(value);
	}

	public CustomAttribute(String name, String[] values) {
		this.name = name;
		this.values = values;
	}

	public CustomAttribute(String name, Set<String> values) {
		this.name = name;
		this.values = values.toArray(new String[0]);
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
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

	public void setValues(Collection<String> values) {
		this.values = values.toArray(new String[0]);
	}

	public void setValues(Set<String> values) {
		this.values = values.toArray(new String[0]);
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
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CustomAttribute that = (CustomAttribute) o;

		return !(name != null ? !name.equalsIgnoreCase(that.name) : that.name != null);

	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return String.format("Attribute [name=%s, values=%s]", name, Arrays.toString(values));
	}

	public int compareTo(CustomAttribute o) {
		return name.compareTo(o.name);
	}

}
