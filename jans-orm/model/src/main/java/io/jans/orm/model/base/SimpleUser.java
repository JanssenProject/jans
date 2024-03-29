/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.util.StringHelper;

/**
 * @author Javier Rojas Blum Date: 11.25.2011
 */
@DataEntry
@ObjectClass
public class SimpleUser extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @AttributeName(name = "uid", consistency = true)
    private String userId;

    @AttributeName
    private Date updatedAt;

    @AttributeName(name = "jansCreationTimestamp")
    private Date createdAt;

    @AttributeName(name = "jansPersistentJWT")
    private String[] oxAuthPersistentJwt;

    @AttributeName(name = "jansExtUid")
    private String[] externalUid;

    @AttributeName(name = "jansAuthenticator")
    private String[] jansAuthenticator;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    protected List<CustomObjectAttribute> customAttributes = new ArrayList<CustomObjectAttribute>();

    @CustomObjectClass
    private String[] customObjectClasses;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String[] getOxAuthPersistentJwt() {
        return oxAuthPersistentJwt;
    }

    public void setOxAuthPersistentJwt(String[] oxAuthPersistentJwt) {
        this.oxAuthPersistentJwt = oxAuthPersistentJwt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String[] getExternalUid() {
		return externalUid;
	}

	public void setExternalUid(String[] externalUid) {
		this.externalUid = externalUid;
	}

	public String[] getJansAuthenticator() {
		return jansAuthenticator;
	}

	public void setJansAuthenticator(String[] jansAuthenticator) {
		this.jansAuthenticator = jansAuthenticator;
	}

	public List<CustomObjectAttribute> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomObjectAttribute> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

    public String getAttribute(String attributeName) {
        Object objectAttribute = getAttributeObject(attributeName);

        return StringHelper.toString(objectAttribute);
    }

    public Object getAttributeObject(String attributeName) {
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

    public List<String> getAttributeValues(String attributeName) {
    	List<Object> objectValues = getAttributeObjectValues(attributeName);
    	if (objectValues == null) {
    		return null;
    	}

    	List<String> values = new ArrayList<String>(objectValues.size());
    	for (Object objectValue : objectValues) {
    		values.add(StringHelper.toString(objectValue));
    	}

        return values;
    }

    public List<Object> getAttributeObjectValues(String attributeName) {
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

    public void setAttribute(String name, Object value) {
        setAttribute(name, value, null);
    }

    public void setAttribute(String name, Object value, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(name, value);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(name);
        getCustomAttributes().add(attribute);
    }

    public void setAttribute(String name, Object[] values) {
        setAttribute(name, values, null);
    }

    public void setAttribute(String name, Object[] values, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(name, Arrays.asList(values));
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(name);
        getCustomAttributes().add(attribute);
    }

    public void setAttribute(String name, List<String> values) {
        setAttribute(name, values, null);
    }

    public void setAttribute(String name, List<String> values, Boolean multiValued) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(name, values);
        if (multiValued != null) {
            attribute.setMultiValued(multiValued);
        }

        removeAttribute(name);
        getCustomAttributes().add(attribute);
    }

    public void removeAttribute(String name) {
        for (Iterator<CustomObjectAttribute> it = getCustomAttributes().iterator(); it.hasNext(); ) {
            if (StringHelper.equalsIgnoreCase(name, it.next().getName())) {
                it.remove();
                break;
            }
        }
    }

}
