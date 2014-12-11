/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.model;

import java.io.Serializable;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.Entry;
import org.xdi.ldap.model.GluuStatus;

/**
 * Attribute Metadata
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
@LdapEntry(sortBy = { "displayName" })
@LdapObjectClass(values = { "top", "gluuAttribute" })
public @Data class GluuAttribute extends Entry implements Serializable {

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

	@NotNull
	@LdapAttribute(name = "gluuAttributePrivacyLevel")
	private GluuAttributePrivacyLevel privacyLevel;

	@LdapAttribute(name = "gluuAttributeUsageType")
	private GluuAttributeUsageType[] usageType;

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
