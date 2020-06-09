/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.service.external.context;

import java.util.Properties;

import org.gluu.persist.PersistenceEntryManager;

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
