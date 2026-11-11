/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.LanguageTag;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.model.base.LocalizedString;

/**
 * Fixture for LDAP style localized attributes: {@code displayName} is mapped to a regular property
 * and also holds default value of {@code displayNameLocalized}, {@code jansUri} has no regular property.
 */
@DataEntry
@ObjectClass("jansTestEntry")
public class LdapStyleLocalizedEntry {

	@DN
	private String dn;

	@AttributeName(name = "displayName")
	private String displayName;

	@AttributeName(name = "displayNameLocalized")
	@LanguageTag
	private LocalizedString displayNameLocalized = new LocalizedString();

	@AttributeName(name = "jansUriLocalized")
	@LanguageTag
	private LocalizedString uriLocalized = new LocalizedString();

	@AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
	private List<CustomObjectAttribute> customAttributes = new ArrayList<>();

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public LocalizedString getDisplayNameLocalized() {
		return displayNameLocalized;
	}

	public void setDisplayNameLocalized(LocalizedString displayNameLocalized) {
		this.displayNameLocalized = displayNameLocalized;
	}

	public LocalizedString getUriLocalized() {
		return uriLocalized;
	}

	public void setUriLocalized(LocalizedString uriLocalized) {
		this.uriLocalized = uriLocalized;
	}

	public List<CustomObjectAttribute> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
		this.customAttributes = customAttributes;
	}

}
