/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.persist.annotation.AttributesList;
import org.gluu.persist.annotation.CustomObjectClass;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 04/08/2014
 */
@DataEntry
@ObjectClass(values = { "top" })
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
