/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.external.context;

import java.util.Properties;

import io.jans.orm.PersistenceEntryManager;

/**
 * Holds object required in persistence scope custom scripts 
 * 
 * @author Yuriy Movchan  Date: 06/05/2020
 */
public class PersistenceExternalContext extends ExternalScriptContext {

    private Properties connectionProperties;
	private PersistenceEntryManager persistenceEntryManager;

	public PersistenceExternalContext() {
		super(null, null);
	}

	public Properties getConnectionProperties() {
		return connectionProperties;
	}

	public void setConnectionProperties(Properties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	public PersistenceEntryManager getPersistenceEntryManager() {
		return persistenceEntryManager;
	}

	public void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}

}
