/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.jans.orm.annotation.AttributeName;
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
@ObjectClass(value = "jansPerson")
public class SimpleCustomStringUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @DN
    private String dn;

    @AttributeName(name = "uid")
    private String userId;

    @AttributeName(name = "userPassword")
    private String userPassword;
    
    @AttributeName(name = "jansRole")
    private UserRole userRole; 

    @AttributeName(name = "memberOf")
    private List<String> memberOf; 

    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    private List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();

    @CustomObjectClass
    private String[] customObjectClasses;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public UserRole getUserRole() {
		return userRole;
	}

	public void setUserRole(UserRole userRole) {
		this.userRole = userRole;
	}

	public List<String> getMemberOf() {
		return memberOf;
	}

	public void setMemberOf(List<String> memberOf) {
		this.memberOf = memberOf;
	}

	public List<CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String getAttribute(String attributeName) {
    	String attribute = null;
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (customAttribute.getName().equals(attributeName)) {
                    attribute = customAttribute.getValue();
                    break;
                }
            }
        }

        return attribute;
    }

    public List<String> getAttributeValues(String attributeName) {
        List<String> values = null;
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                    values = customAttribute.getValues();
                    break;
                }
            }
        }

        return values;
    }

    public void setAttributeValue(String attributeName, String attributeValue) {
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                	customAttribute.setValue(attributeValue);
                    return;
                }
            }
            customAttributes.add(new CustomAttribute(attributeName, attributeValue));
        }
    }

    public void setAttributeValues(String attributeName, List<String> attributeValues) {
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                	customAttribute.setValues(attributeValues);
                    return;
                }
            }
            customAttributes.add(new CustomAttribute(attributeName, attributeValues));
        }
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
