/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.operation;

import java.util.HashMap;
import java.util.Map;

/**
 * Supported SQL DB types
 *
 * @author Yuriy Movchan Date: 09/16/2022
 */
public enum SupportedDbType {

	MYSQL("mysql"),
    POSTGRESQL("postgresql");

	private String dbType;

	private static Map<String, SupportedDbType> MAP_BY_VALUES = new HashMap<String, SupportedDbType>();

    static {
        for (SupportedDbType enumType : values()) {
            MAP_BY_VALUES.put(enumType.dbType, enumType);
        }
    }
	
	SupportedDbType(String dbType) {
		this.dbType = dbType; 
	}

	public static SupportedDbType resolveDbType(String dbType) {
        return MAP_BY_VALUES.get(dbType);
    }

	@Override
    public String toString() {
        return dbType;
    }

}
