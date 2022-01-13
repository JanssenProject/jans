/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.model;

import java.util.Map;

import com.google.cloud.spanner.Type.StructField;

/**
 * Mapping to DB table
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public class TableMapping {

    private final String baseKeyName;
    private final String tableName;
    private final String objectClass;
    private final Map<String, StructField> columTypes;
    private Map<String, TableMapping> childTableMapping;

    public TableMapping(final String baseKeyName, final String tableName, final String objectClass, Map<String, StructField> columTypes) {
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

	public Map<String, StructField> getColumTypes() {
		return columTypes;
	}

	public Map<String, TableMapping> getChildTableMapping() {
		return childTableMapping;
	}

	public void setChildTableMapping(Map<String, TableMapping> childTableMapping) {
		this.childTableMapping = childTableMapping;
	}

	public boolean hasChildTables() {
		return (childTableMapping != null) && (childTableMapping.size() > 0);
	}

	public boolean hasChildTableForAttribute(String attributeName) {
		if (!hasChildTables()) {
			return false;
		}
		
		return childTableMapping.containsKey(attributeName);
	}

	public TableMapping getChildTableMappingForAttribute(String attributeName) {
		if (!hasChildTableForAttribute(attributeName)) {
			return null;
		}
		
		return childTableMapping.get(attributeName);
	}

}
