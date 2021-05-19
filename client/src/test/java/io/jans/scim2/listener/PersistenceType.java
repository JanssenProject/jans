package io.jans.scim2.listener;

import io.jans.util.StringHelper;

import java.util.stream.Stream;

public enum PersistenceType { 
	LDAP, COUCHBASE, SPANNER, MYSQL, PGSQL;

	public static PersistenceType fromString(String from) {
		return Stream.of(PersistenceType.values())
			.filter(bt -> StringHelper.equalsIgnoreCase(bt.name(), from))
			.findFirst().orElse(null);    		
	}
	
}
