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
    private final String type;
    private final Boolean multiValued;

    public AttributeType(String name, String type) {
		this(name, type, null);
	}

	public AttributeType(String name, String type, Boolean multiValued) {
		this.name = name;
		this.type = type;
		this.multiValued = multiValued;
	}

	public Boolean getMultiValued() {
		return multiValued;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "AttributeType [name=" + name + ", type=" + type + ", multiValued=" + multiValued + "]";
	}

}
