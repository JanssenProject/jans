/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.persistence;

/**
 * Runs {@link VersionedUpdateContractTest} against the LDAP backend (task 05), via the RFC 4528
 * assertion control. Skipped when the active {@code -Dcfg} profile's persistence type isn't
 * {@code ldap}.
 *
 * @author Yuriy Movchan Date: 10/02/2026
 */
public class LdapVersionedUpdateContractTest extends VersionedUpdateContractTest {

	@Override
	protected String requiredPersistenceType() {
		return LDAP;
	}

}
