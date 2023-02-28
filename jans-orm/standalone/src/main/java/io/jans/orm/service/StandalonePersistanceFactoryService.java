/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.exception.PropertyNotFoundException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.reflect.util.ReflectHelper;

import static org.reflections.scanners.Scanners.SubTypes;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 05/10/2019
 */
public class StandalonePersistanceFactoryService extends PersistanceFactoryService {

	private HashMap<String, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryNames;
	private HashMap<Class<? extends PersistenceEntryManagerFactory>, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryTypes;
	private Set<String> initializedFactories = new HashSet<>();

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
		initFactory(persistenceEntryManagerFactory);

		return persistenceEntryManagerFactory;
	}

	private void initFactory(PersistenceEntryManagerFactory persistenceEntryManagerFactory) {
		String persistenceType = persistenceEntryManagerFactory.getPersistenceType();
		if (!initializedFactories.contains(persistenceType)) {
			persistenceEntryManagerFactory.initStandalone(this);
			initializedFactories.add(persistenceType);
		}
	}

	@Override
	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType) {
		if (this.persistenceEntryManagerFactoryNames == null) {
			initPersistenceManagerMaps();
		}

		PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.persistenceEntryManagerFactoryNames.get(persistenceType);
		initFactory(persistenceEntryManagerFactory);

		return persistenceEntryManagerFactory;
	}

	private void initPersistenceManagerMaps() {
		this.persistenceEntryManagerFactoryNames = new HashMap<String, PersistenceEntryManagerFactory>();
		this.persistenceEntryManagerFactoryTypes = new HashMap<Class<? extends PersistenceEntryManagerFactory>, PersistenceEntryManagerFactory>();

		org.reflections.Reflections reflections = new org.reflections.Reflections(new org.reflections.util.ConfigurationBuilder()
			     .setUrls(org.reflections.util.ClasspathHelper.forPackage("io.jans.orm"))
                 .setScanners(SubTypes));
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
