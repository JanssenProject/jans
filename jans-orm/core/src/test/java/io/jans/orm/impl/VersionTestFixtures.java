/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;

/**
 * Minimal entity fixtures for {@link BaseEntryManagerVersionTest}.
 */
class VersionTestFixtures {

	@DataEntry
	@ObjectClass("jansTestEntry")
	static class VersionedEntry {

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

	@DataEntry(forceUpdate = true)
	@ObjectClass("jansTestEntry")
	static class ForceUpdateVersionedEntry {

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

	@DataEntry
	@ObjectClass("jansTestEntry")
	static class NoVersionEntry {

		@DN
		private String dn;

		@AttributeName(name = "uid")
		private String uid;

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

	}

	@DataEntry
	@ObjectClass("jansTestEntry")
	static class DoubleVersionEntry {

		@DN
		private String dn;

		@Version
		@AttributeName(name = "jansVersion")
		private Long version;

		@Version
		@AttributeName(name = "jansVersion2")
		private Long version2;

		public String getDn() {
			return dn;
		}

		public void setDn(String dn) {
			this.dn = dn;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public Long getVersion2() {
			return version2;
		}

		public void setVersion2(Long version2) {
			this.version2 = version2;
		}

	}

}
