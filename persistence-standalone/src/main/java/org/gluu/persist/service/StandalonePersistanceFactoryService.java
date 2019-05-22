package org.gluu.persist.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.PropertyNotFoundException;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.reflect.util.ReflectHelper;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 05/10/2019
 * @param <E>
 */
public class StandalonePersistanceFactoryService<E> extends PersistanceFactoryService {

	private HashMap<String, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryNames;
	private HashMap<Class<? extends PersistenceEntryManagerFactory>, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryTypes;

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration) {
        return getPersistenceEntryManagerFactory(persistenceConfiguration.getEntryManagerFactoryType());
    }

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
		if (this.persistenceEntryManagerFactoryTypes == null) {
			initPersistenceManagerMaps();
		}

		PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.persistenceEntryManagerFactoryTypes
				.get(persistenceEntryManagerFactoryClass);

		return persistenceEntryManagerFactory;
	}

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType) {
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

		getLog().info("Found '{}' PersistenceEntryManagerFactory", classes.size());
		
		List<Class<? extends PersistenceEntryManagerFactory>> classesList = new ArrayList<Class<? extends PersistenceEntryManagerFactory>>(classes);
		for (Class<? extends PersistenceEntryManagerFactory> clazz : classesList) {
			getLog().info("Found PersistenceEntryManagerFactory '{}'", clazz);
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = createPersistenceEntryManagerFactoryImpl(clazz);
			persistenceEntryManagerFactoryNames.put(persistenceEntryManagerFactory.getPersistenceType(), persistenceEntryManagerFactory);
			persistenceEntryManagerFactoryTypes.put(clazz, persistenceEntryManagerFactory);
		}
	}

	private PersistenceEntryManagerFactory createPersistenceEntryManagerFactoryImpl(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
		PersistenceEntryManagerFactory persistenceEntryManagerFactory;
		try {
			persistenceEntryManagerFactory = ReflectHelper.createObjectByDefaultConstructor(persistenceEntryManagerFactoryClass);
		} catch (PropertyNotFoundException | IllegalArgumentException | InstantiationException | IllegalAccessException
				| InvocationTargetException e) {
            throw new ConfigurationException(
                    String.format("Failed to create PersistenceEntryManagerFactory by type '%s'!", persistenceEntryManagerFactoryClass));
		}

		return persistenceEntryManagerFactory;
	}

}
