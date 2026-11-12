/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.impl;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;

/**
 * Top-level entity fixture for {@link PersistenceEntryManagerBatchDefaultsTest}: entity creation on read
 * needs a public top-level class with a default constructor.
 */
@DataEntry
@ObjectClass("jansTestEntry")
public class BatchTestEntry {

	@DN
	private String dn;

	@AttributeName(name = "uid")
	private String uid;

	@Version
	@AttributeName(name = "jansVersion")
	private Long version;

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
