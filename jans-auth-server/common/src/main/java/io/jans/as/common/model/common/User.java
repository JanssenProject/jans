/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.common;

import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.StringHelper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yuriy Movchan Date: 06/11/2013
 */
@DataEntry
@ObjectClass(value = "jansPerson")
public class User extends SimpleUser {

    private static final long serialVersionUID = 6634191420188575733L;

    @Deprecated
    public void setAttribute(String attributeName, String attributeValue) {
        setAttribute(attributeName, attributeValue, null);
    }

    public void setAttribute(String attributeName, String attributeValue, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValue);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    @Deprecated
    public void setAttribute(String attributeName, String[] attributeValues) {
        setAttribute(attributeName, attributeValues, null);
    }

    public void setAttribute(String attributeName, String[] attributeValues, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, Arrays.asList(attributeValues));
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    @Deprecated
    public void setAttribute(String attributeName, List<String> attributeValues) {
        setAttribute(attributeName, attributeValues, null);
    }

    public void setAttribute(String attributeName, List<String> attributeValues, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValues);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(attributeName);
        getCustomAttributes().add(attribute);
    }

    public void removeAttribute(String attributeName) {
        for (Iterator<CustomObjectAttribute> it = getCustomAttributes().iterator(); it.hasNext(); ) {
            if (StringHelper.equalsIgnoreCase(attributeName, it.next().getName())) {
                it.remove();
                break;
            }
        }
    }

    public String getStatus() {
        return getAttribute("jansStatus");
    }

}