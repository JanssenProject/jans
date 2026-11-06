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
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.LanguageTag;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.model.base.LocalizedString;

/**
 * Entity fixture for {@link BaseEntryManagerLanguageTagTest}. Top level class: nested classes
 * are instantiated by BaseEntryManager as inner classes.
 */
@DataEntry
@ObjectClass("jansTestEntry")
public class LocalizedEntry {

	@DN
	private String dn;

	@AttributeName(name = "jansUri")
	private String uri;

	@AttributeName(name = "jansUriLocalized")
	@JsonObject
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

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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
