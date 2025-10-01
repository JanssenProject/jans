/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.external;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.jans.service.external.context.PersistenceExternalContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.persistence.PersistenceType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.service.custom.script.ExternalScriptService;

/**
 * Provides factory methods needed to create persistence extension
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
@ApplicationScoped
public class ExternalPersistenceExtensionService extends ExternalScriptService {

	private static final long serialVersionUID = 5466361778036208685L;

	@Inject
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	private Instance<List<PersistenceEntryManager>> persistenceEntryManagerListInstance;

	public ExternalPersistenceExtensionService() {
		super(CustomScriptType.PERSISTENCE_EXTENSION);
	}

	@Override
	protected void reloadExternal() {
		for (Iterator<PersistenceEntryManager> it = persistenceEntryManagerInstance.iterator(); it.hasNext();) {
			PersistenceEntryManager persistenceEntryManager = it.next();
			executePersistenceExtensionAfterCreate(null, persistenceEntryManager);
		}

		for (Iterator<List<PersistenceEntryManager>> it = persistenceEntryManagerListInstance.iterator(); it.hasNext();) {
			List<PersistenceEntryManager> persistenceEntryManagerList = it.next();
			for (PersistenceEntryManager persistenceEntryManager : persistenceEntryManagerList) {
				executePersistenceExtensionAfterCreate(null, persistenceEntryManager);
			}
		}
    }

	public void executePersistenceExtensionAfterCreate(Properties connectionProperties, PersistenceEntryManager persistenceEntryManager) {
		if (isEnabled()) {
			PersistenceExternalContext persistenceExternalContext = new PersistenceExternalContext();
			persistenceExternalContext.setConnectionProperties(connectionProperties);
			persistenceExternalContext.setPersistenceEntryManager(persistenceEntryManager);
			
			executeExternalOnAfterCreateMethod(persistenceExternalContext);
		}

		setPersistenceExtension(persistenceEntryManager);
	}

	public void executePersistenceExtensionAfterDestroy(PersistenceEntryManager persistenceEntryManager) {
		if (isEnabled()) {
			PersistenceExternalContext persistenceExternalContext = new PersistenceExternalContext();
			persistenceExternalContext.setPersistenceEntryManager(persistenceEntryManager);
			
			executeExternalOnAfterDestroyMethod(persistenceExternalContext);
		}
	}

	public void setPersistenceExtension(PersistenceEntryManager persistenceEntryManager) {
		PersistenceExtension persistenceExtension = null;
		if (isEnabled()) {
			persistenceExtension = (PersistenceExtension) this.defaultExternalCustomScript.getExternalType();
		}

		persistenceEntryManager.setPersistenceExtension(persistenceExtension);
	}

	public void executeExternalOnAfterCreateMethod(PersistenceExternalContext context) {
		executeExternalOnAfterCreateMethod(this.defaultExternalCustomScript, context);
	}

	public void executeExternalOnAfterCreateMethod(CustomScriptConfiguration customScriptConfiguration, PersistenceExternalContext context) {
		try {
			log.debug("Executing python 'onAfterCreate' method");
			PersistenceType persistenceType = (PersistenceType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			persistenceType.onAfterCreate(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}
	}

	public void executeExternalOnAfterDestroyMethod(PersistenceExternalContext context) {
		executeExternalOnAfterDestroyMethod(this.defaultExternalCustomScript, context);
	}

	public void executeExternalOnAfterDestroyMethod(CustomScriptConfiguration customScriptConfiguration, PersistenceExternalContext context) {
		try {
			log.debug("Executing python 'onAfterDestroy' method");
			PersistenceType persistenceType = (PersistenceType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			persistenceType.onAfterDestroy(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}
	}

}
