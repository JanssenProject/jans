package org.gluu.persist.service;

import java.util.HashMap;
import java.util.Set;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 05/10/2019
 */
public class StandalonePersistanceFactoryService extends PersistanceFactoryService {

	private HashMap<String, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryNames;
	private HashMap<Class<? extends PersistenceEntryManagerFactory>, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryTypes;

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration) {
        return getPersistenceEntryManagerFactoryImpl(persistenceConfiguration.getEntryManagerFactoryType());
    }

	private PersistenceEntryManagerFactory getPersistenceEntryManagerFactoryImpl(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
		if (this.persistenceEntryManagerFactoryTypes == null) {
			initPersistenceManagerMaps();
		}

		PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.persistenceEntryManagerFactoryTypes
				.get(persistenceEntryManagerFactoryClass);

		return persistenceEntryManagerFactory;
	}

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType) {
		return getPersistenceEntryManagerFactoryImpl(persistenceType);
	}

	private PersistenceEntryManagerFactory getPersistenceEntryManagerFactoryImpl(String persistenceType) {
		if (this.persistenceEntryManagerFactoryNames == null) {
			initPersistenceManagerMaps();
		}

		PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.persistenceEntryManagerFactoryNames.get(persistenceType);

		return persistenceEntryManagerFactory;
	}

	private void initPersistenceManagerMaps() {
		this.persistenceEntryManagerFactoryNames = new HashMap<String, PersistenceEntryManagerFactory>();
		this.persistenceEntryManagerFactoryTypes = new HashMap<Class<? extends PersistenceEntryManagerFactory>, PersistenceEntryManagerFactory>();

		org.reflections.Reflections reflections = new org.reflections.Reflections(new org.reflections.util.ConfigurationBuilder()
			     .setUrls(org.reflections.util.ClasspathHelper.forPackage("org.gluu.persist"))
			     .setScanners(new org.reflections.scanners.SubTypesScanner()));
		Set<Class<? extends PersistenceEntryManagerFactory>> classes = reflections.getSubTypesOf(PersistenceEntryManagerFactory.class);
		
		for (Class<? extends PersistenceEntryManagerFactory> clazz : classes) {
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = getPersistenceEntryManagerFactoryImpl(clazz);
			persistenceEntryManagerFactoryNames.put(persistenceEntryManagerFactory.getPersistenceType(), persistenceEntryManagerFactory);
			persistenceEntryManagerFactoryTypes.put(clazz, persistenceEntryManagerFactory);
		}
	}

}
