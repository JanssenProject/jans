/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.test.persistence.PersistenceTestFixture;
import io.jans.orm.util.StringHelper;

/**
 * Base class for all persistence tests. Provides shared PersistenceEntryManager
 * created from profile configuration. Tests are skipped when profile is not specified.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public abstract class BaseOrmTest {

	public static final String LDAP = "ldap";
	public static final String SQL = "sql";
	public static final String COUCHBASE = "couchbase";
	public static final String SPANNER = "spanner";
	public static final String HYBRID = "hybrid";

	protected PersistenceEntryManager entryManager;
	protected String persistenceType;

	@BeforeClass
	public void initEntryManager() {
		this.entryManager = PersistenceTestFixture.getEntryManager();
		this.persistenceType = PersistenceTestFixture.getBasePersistenceType();
	}

	/*
	 * Skip test class if current profile persistence type is not in the list
	 */
	protected void requirePersistenceType(String... types) {
		for (String type : types) {
			if (StringHelper.equalsIgnoreCase(persistenceType, type)) {
				return;
			}
		}

		throw new SkipException(String.format("Test requires persistence type %s but profile uses '%s'",
				java.util.Arrays.toString(types), persistenceType));
	}

	protected String getRandomInum() {
		return String.format("%s-%04d", System.currentTimeMillis(), (int) (Math.random() * 10000));
	}

}
