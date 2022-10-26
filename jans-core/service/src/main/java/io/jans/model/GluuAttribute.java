/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import io.jans.model.attribute.AttributeDataType;
import io.jans.model.attribute.AttributeValidation;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Attribute Metadata
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version May 2, 2019
 */
@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansAttr")
public class GluuAttribute extends Entry implements Serializable {

	private static final long serialVersionUID = 4817004894646725606L;

	private transient boolean selected;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName(name = "jansSourceAttr")
	private String sourceAttribute;

	@AttributeName(name = "jansNameIdTyp")
	private String nameIdType;

	@NotNull
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Name should contain alphabetical and numeric characters only")
	@Size(min = 1, max = 30, message = "Length of the Name should be between 1 and 30")
	@AttributeName(name = "jansAttrName")
	private String name;

	@NotNull
	@Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
	@AttributeName
	private String displayName;

	@NotNull
	@Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
	@AttributeName
	private String description;

	@AttributeName(name = "jansAttrOrigin")
	private String origin;

	@NotNull
	@AttributeName(name = "jansAttrTyp")
	private AttributeDataType dataType;

	@NotNull
	@AttributeName(name = "jansAttrEditTyp")
	private GluuUserRole[] editType;

	@NotNull
	@AttributeName(name = "jansAttrViewTyp")
	private GluuUserRole[] viewType;

	@AttributeName(name = "jansAttrUsgTyp")
	private GluuAttributeUsageType[] usageType;

	@AttributeName(name = "jansClaimName")
	private String claimName;

	@AttributeName(name = "seeAlso")
	private String seeAlso;

	@AttributeName(name = "jansStatus")
	private GluuStatus status;

	@AttributeName(name = "jansSAML1URI")
	private String saml1Uri;

	@AttributeName(name = "jansSAML2URI")
	private String saml2Uri;

	@AttributeName(ignoreDuringUpdate = true)
	private String urn;

	@AttributeName(name = "jansScimCustomAttr")
	private Boolean scimCustomAttr;

	@AttributeName(name = "jansMultivaluedAttr")
	private Boolean oxMultiValuedAttribute;

    @AttributeName(name = "jansHideOnDiscovery")
    private Boolean jansHideOnDiscovery;

	@Transient
	private boolean custom;

	@Transient
	private boolean requred;

	@JsonObject
	@AttributeName(name = "jansValidation")
	private AttributeValidation attributeValidation;

	@AttributeName(name = "jansTooltip")
	private String tooltip;

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(String sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
	}

	public String getNameIdType() {
		return nameIdType;
	}

	public void setNameIdType(String nameIdType) {
		this.nameIdType = nameIdType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public AttributeDataType getDataType() {
		return dataType;
	}

	public void setDataType(AttributeDataType dataType) {
		this.dataType = dataType;
	}

	public GluuUserRole[] getEditType() {
		return editType;
	}

	public void setEditType(GluuUserRole[] editType) {
		this.editType = editType;
	}

	public GluuUserRole[] getViewType() {
		return viewType;
	}

	public void setViewType(GluuUserRole[] viewType) {
		this.viewType = viewType;
	}

	public GluuAttributeUsageType[] getUsageType() {
		return usageType;
	}

	public void setUsageType(GluuAttributeUsageType[] usageType) {
		this.usageType = usageType;
	}

	public String getClaimName() {
		return claimName;
	}

	public void setClaimName(String claimName) {
		this.claimName = claimName;
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

	public String getSaml1Uri() {
		return saml1Uri;
	}

	public void setSaml1Uri(String saml1Uri) {
		this.saml1Uri = saml1Uri;
	}

	public String getSaml2Uri() {
		return saml2Uri;
	}

	public void setSaml2Uri(String saml2Uri) {
		this.saml2Uri = saml2Uri;
	}

	public String getUrn() {
		return urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	public Boolean getScimCustomAttr() {
		return scimCustomAttr;
	}

	public void setScimCustomAttr(Boolean scimCustomAttr) {
		this.scimCustomAttr = scimCustomAttr;
	}

	public Boolean getOxMultiValuedAttribute() {
		return oxMultiValuedAttribute == null ? false : oxMultiValuedAttribute;
	}

	public void setOxMultiValuedAttribute(Boolean oxMultiValuedAttribute) {
		this.oxMultiValuedAttribute = oxMultiValuedAttribute;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public boolean isRequred() {
		return requred;
	}

	public void setRequred(boolean requred) {
		this.requred = requred;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public boolean allowEditBy(GluuUserRole role) {
		return GluuUserRole.containsRole(editType, role);
	}

	public boolean allowViewBy(GluuUserRole role) {
		return GluuUserRole.containsRole(viewType, role);
	}

	public boolean isAdminCanAccess() {
		return isAdminCanView() | isAdminCanEdit();
	}

	public boolean isAdminCanView() {
		return allowViewBy(GluuUserRole.ADMIN);
	}

	public boolean isAdminCanEdit() {
		return allowEditBy(GluuUserRole.ADMIN);
	}

	public boolean isUserCanAccess() {
		return isUserCanView() | isUserCanEdit();
	}

	public boolean isUserCanView() {
		return allowViewBy(GluuUserRole.USER);
	}

	public boolean isWhitePagesCanView() {
		return allowViewBy(GluuUserRole.WHITEPAGES);
	}

	public boolean isUserCanEdit() {
		return allowEditBy(GluuUserRole.USER);
	}

	public AttributeValidation getAttributeValidation() {
		return attributeValidation;
	}

	public void setAttributeValidation(AttributeValidation attributeValidation) {
		this.attributeValidation = attributeValidation;
	}

    public Boolean getJansHideOnDiscovery() {
        return jansHideOnDiscovery;
    }

    public void setJansHideOnDiscovery(Boolean jansHideOnDiscovery) {
        this.jansHideOnDiscovery = jansHideOnDiscovery;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (custom ? 1231 : 1237);
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + Arrays.hashCode(editType);
		result = prime * result + ((tooltip == null) ? 0 : tooltip.hashCode());
		result = prime * result + ((inum == null) ? 0 : inum.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nameIdType == null) ? 0 : nameIdType.hashCode());
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		result = prime * result + ((claimName == null) ? 0 : claimName.hashCode());
		result = prime * result + ((oxMultiValuedAttribute == null) ? 0 : oxMultiValuedAttribute.hashCode());
		result = prime * result + ((scimCustomAttr == null) ? 0 : scimCustomAttr.hashCode());
		result = prime * result + (requred ? 1231 : 1237);
		result = prime * result + ((saml1Uri == null) ? 0 : saml1Uri.hashCode());
		result = prime * result + ((saml2Uri == null) ? 0 : saml2Uri.hashCode());
		result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
		result = prime * result + ((sourceAttribute == null) ? 0 : sourceAttribute.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((urn == null) ? 0 : urn.hashCode());
		result = prime * result + Arrays.hashCode(usageType);
		result = prime * result + Arrays.hashCode(viewType);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof GluuAttribute)) {
			return false;
		}
		GluuAttribute other = (GluuAttribute) obj;
		if (inum == null) {
			if (other.inum != null)
				return false;
		} else if (!inum.equals(other.inum))
			return false;
		if (custom != other.custom)
			return false;
		if (dataType != other.dataType)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		return true;
	}

}