/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jans.orm.model.AttributeData;

/**
 * One row of an atomic batch insert: entry DN, SQL key ({@code doc_id}), base object class (table)
 * and the attributes already converted for SQL, exactly as a single insert gets them.
 *
 * @author Yuriy Movchan Date: 11/12/2026
 */
public final class BatchInsert {

	private final String dn;
	private final String key;
	private final String objectClass;
	private final List<AttributeData> attributes;

	public BatchInsert(String dn, String key, String objectClass, List<AttributeData> attributes) {
		this.dn = dn;
		this.key = key;
		this.objectClass = objectClass;
		this.attributes = attributes == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(attributes));
	}

	public String getDn() {
		return dn;
	}

	public String getKey() {
		return key;
	}

	public String getObjectClass() {
		return objectClass;
	}

	public List<AttributeData> getAttributes() {
		return attributes;
	}

}
