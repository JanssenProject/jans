/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

import java.util.List;

/**
 * DB row with attribues
 *
 * @author Yuriy Movchan Date: 01/19/2021
 */
public class EntryData {
    private final List<AttributeData> attributeData;

    public EntryData(List<AttributeData> attributeData) {
        this.attributeData = attributeData;
    }

	public List<AttributeData> getAttributeData() {
		return attributeData;
	}

	@Override
	public String toString() {
		return "EntryData [attributeData=" + attributeData + "]";
	}

	public AttributeData getAttributeDate(String internalAttribute) {
		if (attributeData == null) {
			return null;
		}
		
		for (AttributeData attributeData : attributeData) {
			if (internalAttribute.equalsIgnoreCase(attributeData.getName())) {
				return attributeData;
			}
		}

		return null;
	}

}
