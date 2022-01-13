/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON DB value
 *
 * @author Yuriy Movchan Date: 03/02/2021
 */
public class JsonAttributeValue {

	@JsonProperty("v")
    private Object[] values;

	public JsonAttributeValue() {
	}

	public JsonAttributeValue(Object[] values) {
		this.values = values;
	}

	public Object[] getValues() {
		return values;
	}

	public void setValues(Object[] values) {
		this.values = values;
	}

}
