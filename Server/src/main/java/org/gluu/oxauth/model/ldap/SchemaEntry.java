/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.persist.annotation.AttributeName;

/**
 * Schema attribute
 *
 * @author Yuriy Movchan Date: 10.14.2010
 */
@org.gluu.persist.annotation.SchemaEntry
public final class SchemaEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = 3819004894646725606L;

	@AttributeName
	private List<String> attributeTypes = new ArrayList<String>();

	@AttributeName
	private List<String> objectClasses = new ArrayList<String>();

	public final List<String> getAttributeTypes() {
		return attributeTypes;
	}

	public final void setAttributeTypes(List<String> attributeTypes) {
		this.attributeTypes = attributeTypes;
	}

	public final void addAttributeType(String attributeType) {
		this.attributeTypes.add(attributeType);
	}

	public final List<String> getObjectClasses() {
		return objectClasses;
	}

	public final void setObjectClasses(List<String> objectClasses) {
		this.objectClasses = objectClasses;
	}

	public final void addObjectClass(String objectClass) {
		this.objectClasses.add(objectClass);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((attributeTypes == null) ? 0 : attributeTypes.hashCode());
		result = prime * result + ((objectClasses == null) ? 0 : objectClasses.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SchemaEntry other = (SchemaEntry) obj;
		if (attributeTypes == null) {
			if (other.attributeTypes != null) {
				return false;
			}
		} else if (!attributeTypes.equals(other.attributeTypes)) {
			return false;
		}
		if (objectClasses == null) {
			if (other.objectClasses != null) {
				return false;
			}
		} else if (!objectClasses.equals(other.objectClasses)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("SchemaAttribute [dn=%s, attributeTypes=%s, objectClasses=%s]", getDn(), attributeTypes, objectClasses);
	}

}
