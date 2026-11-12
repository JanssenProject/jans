/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jans.orm.model.AttributeData;

/**
 * Immutable result of mapping one entry for insert, as {@code BaseEntryManager.persist(Object)} does it:
 * DN, object classes, attributes (object classes included) and expiration. Backends that write
 * several entries in one operation (for example an atomic batch) use it to reuse the persist mapping.
 */
public final class PreparedInsert {

	private final String dn;
	private final String[] objectClasses;
	private final List<AttributeData> attributes;
	private final Integer expiration;

	public PreparedInsert(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration) {
		this.dn = dn;
		this.objectClasses = objectClasses == null ? null : objectClasses.clone();
		this.attributes = attributes == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(attributes));
		this.expiration = expiration;
	}

	public String getDn() {
		return dn;
	}

	public String[] getObjectClasses() {
		return objectClasses == null ? null : objectClasses.clone();
	}

	public List<AttributeData> getAttributes() {
		return attributes;
	}

	public Integer getExpiration() {
		return expiration;
	}

}
