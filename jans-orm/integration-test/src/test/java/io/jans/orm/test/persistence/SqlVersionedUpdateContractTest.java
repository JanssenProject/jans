/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.persistence;

/**
 * Runs {@link VersionedUpdateContractTest} against the SQL backend (task 04). Skipped when the
 * active {@code -Dcfg} profile's persistence type isn't {@code sql}.
 *
 * @author Yuriy Movchan Date: 10/02/2026
 */
public class SqlVersionedUpdateContractTest extends VersionedUpdateContractTest {

	@Override
	protected String requiredPersistenceType() {
		return SQL;
	}

}
