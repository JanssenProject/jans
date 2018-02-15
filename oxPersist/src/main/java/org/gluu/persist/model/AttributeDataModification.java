/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model;

/**
 * LDAP Attribute
 *
 * @author Yuriy Movchan Date: 10.10.2010
 */
public class AttributeDataModification {

    public enum AttributeModificationType {
        ADD, REMOVE, REPLACE;
    }

    private final AttributeModificationType modificationType;
    private final AttributeData attribute;
    private final AttributeData oldAttribute;

    public AttributeDataModification(AttributeModificationType modificationType, AttributeData attribute) {
        this(modificationType, attribute, null);
    }

    public AttributeDataModification(AttributeModificationType modificationType, AttributeData attribute, AttributeData oldAttribute) {
        this.modificationType = modificationType;
        this.attribute = attribute;
        this.oldAttribute = oldAttribute;
    }

    public final AttributeModificationType getModificationType() {
        return modificationType;
    }

    public final AttributeData getOldAttribute() {
        return oldAttribute;
    }

    public final AttributeData getAttribute() {
        return attribute;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modificationType == null) ? 0 : modificationType.hashCode());
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((oldAttribute == null) ? 0 : oldAttribute.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttributeDataModification other = (AttributeDataModification) obj;
        if (modificationType == null) {
            if (other.modificationType != null) {
                return false;
            }
        } else if (!modificationType.equals(other.modificationType)) {
            return false;
        }
        if (attribute == null) {
            if (other.attribute != null) {
                return false;
            }
        } else if (!attribute.equals(other.attribute)) {
            return false;
        }
        if (oldAttribute == null) {
            if (other.oldAttribute != null) {
                return false;
            }
        } else if (!oldAttribute.equals(other.oldAttribute)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("AttributeModification [modificationType=%s, attribute=%s, oldAttribute=%s]", modificationType, attribute,
                oldAttribute);
    }

}
