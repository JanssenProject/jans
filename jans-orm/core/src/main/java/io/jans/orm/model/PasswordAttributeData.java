/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.model;

/**
 * DB Password Attribute
 *
 * @author Yuriy Movchan Date: 0612/2025
 */
public class PasswordAttributeData extends AttributeData {

	private boolean skipHashed;

	public PasswordAttributeData(AttributeData attributeData, boolean skipHashed) {
		super(attributeData.getName(), attributeData.getValues(), attributeData.getMultiValued(), attributeData.getJsonValue());
		this.skipHashed = skipHashed;
	}

	public boolean isSkipHashed() {
		return skipHashed;
	}

	public void setSkipHashed(boolean skipHashed) {
		this.skipHashed = skipHashed;
	}

}
