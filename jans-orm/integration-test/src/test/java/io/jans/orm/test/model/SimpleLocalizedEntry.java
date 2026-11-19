/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.model;

import java.io.Serializable;
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
 * Sample entry with localized attributes: {@code @LanguageTag} together with {@code @JsonObject}
 * (as in Client) plus {@code @AttributesList} which must not receive localized attributes.
 *
 * @author Yuriy Movchan
 */
@DataEntry
@ObjectClass(value = "jansTestLocalized")
public class SimpleLocalizedEntry implements Serializable {

    private static final long serialVersionUID = 4160182391412260921L;

    @DN
    private String dn;

    @AttributeName(name = "displayName")
    private String displayName;

    @AttributeName(name = "displayNameLocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString displayNameLocalized = new LocalizedString();

    @AttributeName(name = "jansClntURI")
    private String clientUri;

    @AttributeName(name = "jansClntURILocalized")
    @JsonObject
    @LanguageTag
    private LocalizedString clientUriLocalized = new LocalizedString();

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

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public LocalizedString getClientUriLocalized() {
        return clientUriLocalized;
    }

    public void setClientUriLocalized(LocalizedString clientUriLocalized) {
        this.clientUriLocalized = clientUriLocalized;
    }

    public List<CustomObjectAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

}
