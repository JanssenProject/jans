/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.jans.model.GluuStatus;
import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Group
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansGrp")
@JsonInclude(Include.NON_NULL)
public class GluuGroup extends Entry implements Serializable {

	private static final long serialVersionUID = -2812480357430436503L;

	private transient boolean selected;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@NotNull
	@Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
	@AttributeName
	private String displayName;

	@Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
	@AttributeName
	private String description;

	@NotNull
	@AttributeName
	private String owner;

	@AttributeName(name = "member")
	private List<String> members = new ArrayList<>();

	@AttributeName(name = "c")
	private String countryName;

	@AttributeName(name = "o")
	private String organization;

	@AttributeName
	private String seeAlso;

	@AttributeName(name = "jansStatus")
	private GluuStatus status;

	@AttributesList(name = "name", value = "values", sortByName = true, attributesConfiguration = {
			@AttributeName(name = "inum", ignoreDuringUpdate = true) })
	private List<GluuCustomAttribute> customAttributes = new ArrayList<GluuCustomAttribute>();

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public List<String> getMembers() {
		return members;
	}

	public void setMembers(List<String> members) {
		this.members = members;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getSeeAlso() {
		return seeAlso;
	}

	public void setSeeAlso(String seeAlso) {
		this.seeAlso = seeAlso;
	}

	public GluuStatus getStatus() {
		return status;
	}

	public void setStatus(GluuStatus status) {
		this.status = status;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public String toString() {
		return String.format(
				"GluuGroup [countryName=%s, description=%s, displayName=%s,  inum=%s, members=%s, organization=%s, owner=%s, seeAlso=%s, status=%s, toString()=%s]",
				countryName, description, displayName, inum, members, organization, owner, seeAlso, status,
				super.toString());
	}

	public List<GluuCustomAttribute> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(List<GluuCustomAttribute> customAttributes) {
		this.customAttributes = customAttributes;
	}

	public String getAttribute(String attributeName) {
		String value = null;
		for (GluuCustomAttribute attribute : customAttributes) {
			if (attribute.getName().equalsIgnoreCase(attributeName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	public String[] getAttributeArray(String attributeName) {
		GluuCustomAttribute gluuCustomAttribute = getGluuCustomAttribute(attributeName);
		if (gluuCustomAttribute == null) {
			return null;
		} else {
			return gluuCustomAttribute.getValues();
		}
	}

	public GluuCustomAttribute getGluuCustomAttribute(String attributeName) {
		for (GluuCustomAttribute gluuCustomAttribute : customAttributes) {
			if (gluuCustomAttribute.getName().equalsIgnoreCase(attributeName)) {
				return gluuCustomAttribute;
			}
		}

		return null;
	}

	public void setAttribute(String attributeName, String attributeValue) {
		GluuCustomAttribute attribute = new GluuCustomAttribute(attributeName, attributeValue);
		customAttributes.remove(attribute);
		customAttributes.add(attribute);
	}

	public void setAttribute(String attributeName, String[] attributeValue) {
		GluuCustomAttribute attribute = new GluuCustomAttribute(attributeName, attributeValue);
		customAttributes.remove(attribute);
		customAttributes.add(attribute);
	}
}
