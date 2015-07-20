/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.model;

import java.io.Serializable;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.Entry;
import org.xdi.ldap.model.GluuStatus;

/**
 * Attribute Metadata
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version February 9, 2015
 */
@LdapEntry(sortBy = {"displayName"})
@LdapObjectClass(values = {"top", "gluuAttribute"})
public class GluuAttribute extends Entry implements Serializable {

    private static final long serialVersionUID = 4817004894646725606L;

    private transient boolean selected;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "oxAttributeType")
    private String type;

    @LdapAttribute
    private String lifetime;

    @LdapAttribute(name = "oxSourceAttribute")
    private String sourceAttribute;

    @LdapAttribute
    private String salt;

    @LdapAttribute(name = "oxNameIdType")
    private String nameIdType;

    @NotNull
    @Pattern(regexp = "^[a-zA-Z_]+$", message = "Name should contain only letters and underscores")
    @Size(min = 1, max = 30, message = "Length of the Name should be between 1 and 30")
    @LdapAttribute(name = "gluuAttributeName")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @LdapAttribute
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @LdapAttribute
    private String description;

    @LdapAttribute(name = "gluuAttributeOrigin")
    private String origin;

    @NotNull
    @LdapAttribute(name = "gluuAttributeType")
    private GluuAttributeDataType dataType;

    @NotNull
    @LdapAttribute(name = "gluuAttributeEditType")
    private GluuUserRole[] editType;

    @NotNull
    @LdapAttribute(name = "gluuAttributeViewType")
    private GluuUserRole[] viewType;

    @LdapAttribute(name = "gluuAttributeUsageType")
    private GluuAttributeUsageType[] usageType;

    @LdapAttribute(name = "oxAuthClaimName")
    private String oxAuthClaimName;

    @LdapAttribute(name = "seeAlso")
    private String seeAlso;

    @LdapAttribute(name = "gluuStatus")
    private GluuStatus status;

    @LdapAttribute(name = "gluuSAML1URI")
    private String saml1Uri;

    @LdapAttribute(name = "gluuSAML2URI")
    private String saml2Uri;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String urn;

    @LdapAttribute(name = "oxSCIMCustomAttribute")
    private ScimCustomAtribute oxSCIMCustomAttribute;

    @LdapAttribute(name = "oxMultivaluedAttribute")
    private OxMultivalued oxMultivaluedAttribute;

    @Transient
    private boolean custom;

    @Transient
    private boolean requred;

    @LdapAttribute(name = "gluuRegExp")
    private String regExp;

    @LdapAttribute(name = "gluuTooltip")
    private String gluuTooltip;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLifetime() {
		return lifetime;
	}

	public void setLifetime(String lifetime) {
		this.lifetime = lifetime;
	}

	public String getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(String sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
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

	public GluuAttributeDataType getDataType() {
		return dataType;
	}

	public void setDataType(GluuAttributeDataType dataType) {
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

	public String getOxAuthClaimName() {
		return oxAuthClaimName;
	}

	public void setOxAuthClaimName(String oxAuthClaimName) {
		this.oxAuthClaimName = oxAuthClaimName;
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

	public ScimCustomAtribute getOxSCIMCustomAttribute() {
		return oxSCIMCustomAttribute;
	}

	public void setOxSCIMCustomAttribute(ScimCustomAtribute oxSCIMCustomAttribute) {
		this.oxSCIMCustomAttribute = oxSCIMCustomAttribute;
	}

	public OxMultivalued getOxMultivaluedAttribute() {
		return oxMultivaluedAttribute;
	}

	public void setOxMultivaluedAttribute(OxMultivalued oxMultivaluedAttribute) {
		this.oxMultivaluedAttribute = oxMultivaluedAttribute;
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

	public String getRegExp() {
		return regExp;
	}

	public void setRegExp(String regExp) {
		this.regExp = regExp;
	}

	public String getGluuTooltip() {
		return gluuTooltip;
	}

	public void setGluuTooltip(String gluuTooltip) {
		this.gluuTooltip = gluuTooltip;
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
}
