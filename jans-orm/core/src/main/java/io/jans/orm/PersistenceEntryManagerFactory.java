/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm;

import io.jans.orm.service.BaseFactoryService;

import java.util.Map;
import java.util.Properties;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 02/02/2018
 */
public interface PersistenceEntryManagerFactory {

	void initStandalone(BaseFactoryService persistanceFactoryService);

    String getPersistenceType();

    Map<String, String> getConfigurationFileNames(String alias);

    PersistenceEntryManager createEntryManager(Properties conf);

}
