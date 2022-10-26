/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

/**
 * PostgreSQL JSON column type support
 *
 * @author Yuriy Movchan Date: 09/01/2022
 */

public class JsonString {

	private String value;

	public JsonString(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
