/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.Map;

import io.jans.orm.model.AttributeType;

/**
 * Mapping to DB table
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public class TableMapping {

    private final String baseKeyName;
    private final String tableName;
    private final String objectClass;
    private final Map<String, AttributeType> columTypes;

    public TableMapping(final String baseKeyName, final String tableName, final String objectClass, Map<String, AttributeType> columTypes) {
        this.baseKeyName = baseKeyName;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.columTypes = columTypes;
    }

	public String getBaseKeyName() {
		return baseKeyName;
	}

	public String getTableName() {
		return tableName;
	}

	public String getObjectClass() {
		return objectClass;
	}

	public Map<String, AttributeType> getColumTypes() {
		return columTypes;
	}

}
