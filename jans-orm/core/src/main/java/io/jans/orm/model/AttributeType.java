/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

/**
 * DB Attribute Type
 *
 * @author Yuriy Movchan Date: 07/04/2022
 */
public class AttributeType {
    private final String name;
    private final String defName;
    private final String type;
    private final Boolean multiValued;

    public AttributeType(String defName, String name, String type) {
		this(defName, name, type, null);
	}

	public AttributeType(String defName, String name, String type, Boolean multiValued) {
		this.defName = defName;
		this.name = name;
		this.type = type;
		this.multiValued = multiValued;
	}

	public Boolean getMultiValued() {
		return multiValued;
	}

	public String getDefName() {
		return defName;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "AttributeType [name=" + name + ", defName=" + defName + ", type=" + type + ", multiValued="
				+ multiValued + "]";
	}

}
