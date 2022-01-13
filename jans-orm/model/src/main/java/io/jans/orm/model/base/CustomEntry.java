/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 04/08/2014
 */
@DataEntry
public class CustomEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -7686468010219068788L;

    @AttributesList(name = "name", value = "values", sortByName = true)
    private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @CustomObjectClass
    private String[] customObjectClasses;

    public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public String getCustomAttributeValue(String attributeName) {
        if (customAttributes == null) {
            return null;
        }

        for (CustomAttribute customAttribute : customAttributes) {
            if (StringHelper.equalsIgnoreCase(attributeName, customAttribute.getName())) {
                return customAttribute.getValue();
            }
        }

        return null;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
