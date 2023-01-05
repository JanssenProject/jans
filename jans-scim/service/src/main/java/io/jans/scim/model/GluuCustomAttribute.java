/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.jans.model.GluuAttribute;
import io.jans.model.attribute.AttributeDataType;

/**
 * Attribute
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({GluuAttribute.class})
public class GluuCustomAttribute implements Serializable, Comparable<GluuCustomAttribute> {

	private static final long serialVersionUID = 1468440094325406153L;

	private String name;
	private String[] values;

	private transient GluuAttribute metadata;

	private transient boolean newAttribute = false;

	private transient boolean mandatory = false;
	
	private transient boolean readonly = false;

	private transient GluuBoolean[] booleanValues;
	private transient boolean usedBooleanValues = false;

	public GluuCustomAttribute() {
	}

	public GluuCustomAttribute(String name, String value) {
		this.name = name;
		setValue(value);
	}

	public GluuCustomAttribute(String name, String value, boolean newAttribute) {
		this.name = name;
		setValue(value);
		this.newAttribute = newAttribute;
	}

	public GluuCustomAttribute(String name, String value, boolean newAttribute, boolean mandatory) {
		this.name = name;
		setValue(value);
		this.newAttribute = newAttribute;
		this.mandatory = mandatory;
	}
	public GluuCustomAttribute(String name, String[] values, boolean newAttribute, boolean mandatory) {
		this.name = name;
		this.values = values;
		this.newAttribute = newAttribute;
		this.mandatory = mandatory;
	}
	public GluuCustomAttribute(String name, String[] values) {
		this.name = name;
		this.values = values;
	}

	// To avoid extra code in CR interceptor script
	public GluuCustomAttribute(String name, Set<String> values) {
		this.name = name;
		this.values = values.toArray(new String[0]);
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

	public GluuBoolean getBooleanValue() {
		if (this.booleanValues == null) {
			return null;
		}

		if (this.booleanValues.length > 0) {
			return this.booleanValues[0];
		}

		return null;
	}

	public void setBooleanValue(GluuBoolean value) {
		if (this.booleanValues == null) {
			this.booleanValues = new GluuBoolean[0];
		}

		if (this.booleanValues.length != 1) {
			this.booleanValues = new GluuBoolean[1];
		}
		this.booleanValues[0] = value;
	}

	public String[] getValues() {
		if (this.metadata != null) {
			if ((AttributeDataType.BOOLEAN == this.metadata.getDataType()) && this.usedBooleanValues) {
				this.values = toStringValuesFromBooleanValues(this.booleanValues);
			}
		}

		return values;
	}

	public GluuBoolean[] getBooleanValues() {
		this.usedBooleanValues = true; // Remove after adding separate type for status

		return this.booleanValues;
	}

	public void setBooleanValues(GluuBoolean[] booleanValues) {
		this.usedBooleanValues = true; // Remove after adding separate type for status

		this.booleanValues = booleanValues;
	}
        
        //@com.fasterxml.jackson.annotation.JsonIgnore
        @JsonIgnore
        @XmlTransient
	public void setValues(String[] values) {
		this.values = values;
	}

	public void setValues(Collection<String> values) {
		this.values = values.toArray(new String[0]);
	}

	// To avoid extra code in CR interceptor script
        //@com.fasterxml.jackson.annotation.JsonIgnore
        @JsonIgnore
        @XmlTransient
	public void setValues(Set<String> values) {
		this.values = values.toArray(new String[0]);
	}



	public boolean isNew() {
		return newAttribute;
	}

	public void setNew(boolean newAttribute) {
		this.newAttribute = newAttribute;
	}


	public String getDisplayValue() {

		if (values == null || values.length==0) {
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

	public boolean isAdminCanAccess() {
		return (this.metadata != null) && this.metadata.isAdminCanAccess();
	}

	public boolean isAdminCanView() {
		return (this.metadata != null) && this.metadata.isAdminCanView();
	}

	public boolean isAdminCanEdit() {
		return (this.metadata != null) && this.metadata.isAdminCanEdit();
	}

	public boolean isUserCanAccess() {
		return (this.metadata != null) && this.metadata.isUserCanAccess();
	}

	public boolean isUserCanView() {
		return (this.metadata != null) && this.metadata.isUserCanView();
	}

	public boolean isUserCanEdit() {
		return (this.metadata != null) && this.metadata.isUserCanEdit();
	}
	
	public boolean isMultiValued() {
		return (this.metadata != null) && this.metadata.getOxMultiValuedAttribute();
	}
	
	public void setMultiValued(boolean multiValued) {
		// Needed for ORM
	}

	// public boolean equals(Object attribute) {
	// return (attribute instanceof GluuCustomAttribute) &&
	// (((GluuCustomAttribute) attribute).getName().equals(getName()));
	// }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GluuAttribute getMetadata() {
		return metadata;
	}

	public void setMetadata(GluuAttribute metadata) {
		this.metadata = metadata;
		
		if (this.metadata != null) {
			if (AttributeDataType.BOOLEAN == this.metadata.getDataType()) {
				this.booleanValues = toBooleanValuesFromStringValues(this.values);
			}
		}
	}

	public boolean isNewAttribute() {
		return newAttribute;
	}

	public void setNewAttribute(boolean newAttribute) {
		this.newAttribute = newAttribute;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GluuCustomAttribute that = (GluuCustomAttribute) o;

		return !(name != null ? !name.equalsIgnoreCase(that.name) : that.name != null);

	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return String.format("Attribute [name=%s, values=%s, metadata=%s]", name, Arrays.toString(values), metadata);
	}
	
	public int compareTo(GluuCustomAttribute o) {
		return name.compareTo(o.name);
	}
	/*
	 * Because we are using same id(custId) for all input fields hence using
	 * autogenerated id of an input field to check equal value for multiple
	 * input field
	 */
	Map<String[], String> idComponentMap = new HashMap<String[], String>();

	private GluuBoolean[] toBooleanValuesFromStringValues(String[] inputValues) {
		if (inputValues == null) {
			return null;
		}

		GluuBoolean[] resultValues = new GluuBoolean[inputValues.length];
		for (int i = 0; i < inputValues.length; i++) {
			resultValues[i] = toBooleanFromString(inputValues[i]);
		}
		
		return resultValues;
	}

	private String[] toStringValuesFromBooleanValues(GluuBoolean[] inputValues) {
		if (inputValues == null) {
			return null;
		}

		String resultValues[] = new String[inputValues.length];
		for (int i = 0; i < inputValues.length; i++) {
			resultValues[i] = toStringFromBoolean(inputValues[i]);
		}
		
		return resultValues;
	}

	protected GluuBoolean toBooleanFromString(String value) {
		if (value == null) {
			return null;
		}

		return GluuBoolean.getByValue(value);
	}

	protected String toStringFromBoolean(GluuBoolean value) {
		if (value == null) {
			return null;
		}

		return value.getValue();
	}

}
