/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.util.StringHelper;

/**
* @author Yuriy Movchan Date: 01/15/2020
 */
@DataEntry
@ObjectClass(value = "jansGrant")
public class SimpleGrant implements Serializable {

    private static final long serialVersionUID = -1234191420188575733L;

    @DN
    private String dn;

    @AttributesList(name = "name", value = "values", sortByName = true)
    private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @CustomObjectClass
    private String[] customObjectClasses;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String getAttribute(String ldapAttribute) {
        String attribute = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (customAttribute.getName().equals(ldapAttribute)) {
                    attribute = customAttribute.getValue();
                    break;
                }
            }
        }

        return attribute;
    }

    public List<String> getAttributeValues(String ldapAttribute) {
        List<String> values = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), ldapAttribute)) {
                    values = customAttribute.getValues();
                    break;
                }
            }
        }

        return values;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
