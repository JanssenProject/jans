/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.service;

import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import org.slf4j.Logger;

public interface BaseFactoryService {

	PersistenceConfiguration loadPersistenceConfiguration();

	PersistenceConfiguration loadPersistenceConfiguration(String applicationPropertiesFile);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(
			Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType);

	String getBasePersistenceType(String persistenceType);

	String getPersistenceTypeAlias(String persistenceType);

	Logger getLog();

}