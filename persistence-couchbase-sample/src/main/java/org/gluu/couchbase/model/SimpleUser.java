/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.couchbase.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.AttributesList;
import org.gluu.persist.annotation.CustomObjectClass;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.persist.model.base.CustomObjectAttribute;
import org.gluu.util.StringHelper;

/**
 * @author Yuriy Movchan
 * Date: 11/03/2016
 */
@DataEntry
@ObjectClass(value = "gluuPerson")
public class SimpleUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @DN
    private String dn;

    @AttributeName(name = "uid")
    private String userId;

    @AttributeName(name = "userPassword")
    private String userPassword;
    
    @AttributeName(name = "role")
    private UserRole userRole; 

    @AttributeName(name = "notes")
    private List<String> notes; 

    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    private List<CustomObjectAttribute> customAttributes = new ArrayList<CustomObjectAttribute>();

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

	public List<String> getNotes() {
		return notes;
	}

	public void setNotes(List<String> notes) {
		this.notes = notes;
	}

	public List<CustomObjectAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Object getAttribute(String attributeName) {
    	Object attribute = null;
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomObjectAttribute customAttribute : customAttributes) {
                if (customAttribute.getName().equals(attributeName)) {
                    attribute = customAttribute.getValue();
                    break;
                }
            }
        }

        return attribute;
    }

    public List<Object> getAttributeValues(String attributeName) {
        List<Object> values = null;
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomObjectAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                    values = customAttribute.getValues();
                    break;
                }
            }
        }

        return values;
    }

    public void setAttributeValue(String attributeName, Object attributeValue) {
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomObjectAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                	customAttribute.setValue(attributeValue);
                    return;
                }
            }
            customAttributes.add(new CustomObjectAttribute(attributeName, attributeValue));
        }
    }

    public void setAttributeValues(String attributeName, List<Object> attributeValues) {
        if (attributeName != null && !attributeName.isEmpty()) {
            for (CustomObjectAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(customAttribute.getName(), attributeName)) {
                	customAttribute.setValues(attributeValues);
                    return;
                }
            }
            customAttributes.add(new CustomObjectAttribute(attributeName, attributeValues));
        }
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

}
