package io.jans.configapi.listener;

import io.jans.util.StringHelper;

import java.util.stream.Stream;

public enum PersistenceType { 
	LDAP, COUCHBASE, SPANNER, SQL;

	public static PersistenceType fromString(String from) {
		return Stream.of(PersistenceType.values())
			.filter(bt -> StringHelper.equalsIgnoreCase(bt.name(), from))
			.findFirst().orElse(null);    		
	}
	
}
