/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.lang.reflect.Field;

import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;

/**
 * Builds an {@link OrmTraceStore} outside CDI for tests, wiring its {@code @Inject} fields by
 * reflection (the same approach as {@code EventKindValidatorsTest}) so the production class carries
 * no test-only setters.
 *
 * @author Yuriy Movchan
 */
final class OrmTraceStores {

	static final String BASE_DN = "ou=trace,ou=lock,o=jans";

	private OrmTraceStores() {
	}

	static OrmTraceStore create(PersistenceEntryManager persistenceEntryManager) {
		BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
		baseDnConfiguration.setTrace(BASE_DN);
		StaticConfiguration staticConfiguration = new StaticConfiguration();
		staticConfiguration.setBaseDn(baseDnConfiguration);

		OrmTraceStore store = new OrmTraceStore();
		setField(store, "log", LoggerFactory.getLogger(OrmTraceStore.class));
		setField(store, "persistenceEntryManager", persistenceEntryManager);
		setField(store, "staticConfiguration", staticConfiguration);
		store.init();
		return store;
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
