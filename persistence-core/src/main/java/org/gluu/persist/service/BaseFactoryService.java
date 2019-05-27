package org.gluu.persist.service;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;
import org.slf4j.Logger;

public interface BaseFactoryService {

	PersistenceConfiguration loadPersistenceConfiguration();

	PersistenceConfiguration loadPersistenceConfiguration(String applicationPropertiesFile);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(
			Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass);

	PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType);

	Logger getLog();

}