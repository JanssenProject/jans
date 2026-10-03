/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;

/**
 * Minimal entity dedicated to {@code @Version}/{@code updateWithVersion} CAS integration tests.
 * Requires task 03's {@code jansVersion} column to exist on the backing table before these
 * tests can run against a given profile.
 */
@DataEntry
@ObjectClass("jansTestVersioned")
public class VersionedTestEntry implements Serializable {

	private static final long serialVersionUID = 1L;

	@DN
	private String dn;

	@AttributeName(name = "uid")
	private String uid;

	@AttributeName(name = "dat")
	private String data;

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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
