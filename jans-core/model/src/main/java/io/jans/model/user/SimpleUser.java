/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.jans.model.GluuStatus;
import io.jans.model.user.authenticator.UserAuthenticatorList;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import io.jans.orm.model.base.CustomObjectAttribute;
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

    @JsonObject
    @AttributeName(name = "jansAuthenticator")
    private UserAuthenticatorList authenticator;

    @AttributeName(name = "jansStatus")
    private GluuStatus status;

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

    public GluuStatus getStatus() {
		return status;
	}

	public void setStatus(GluuStatus status) {
		this.status = status;
	}

	public String[] getExternalUid() {
		return externalUid;
	}

	public void setExternalUid(String[] externalUid) {
		this.externalUid = externalUid;
	}

	public UserAuthenticatorList getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(UserAuthenticatorList authenticator) {
		this.authenticator = authenticator;
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

        // This code part we need to remove in future. It's for compatibility with existing scripts.
        if ((objectAttribute == null) && attributeName.equalsIgnoreCase("jansExtUid")) {
        	String[] externalUids = getExternalUid();
        	if ((externalUids != null) && (externalUids.length > 0)) {
        		return externalUids[0];
        	}
        }

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
            // This code part we need to remove in future. It's for compatibility with existing scripts.
            if (attributeName.equalsIgnoreCase("jansExtUid")) {
            	String[] externalUids = getExternalUid();
            	if ((externalUids != null) && (externalUids.length > 0)) {
            		return Arrays.asList(externalUids);
            	}
            }

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

    public void removeAttributeValue(String name) {
        for (Iterator<CustomObjectAttribute> it = getCustomAttributes().iterator(); it.hasNext(); ) {
        	CustomObjectAttribute customObjectAttribute = it.next();
            if (StringHelper.equalsIgnoreCase(name, customObjectAttribute.getName())) {
            	customObjectAttribute.setValue(null);
                break;
            }
        }
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
