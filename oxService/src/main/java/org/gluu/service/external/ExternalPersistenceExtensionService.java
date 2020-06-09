/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.service.external;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.persistence.PersistenceType;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.extension.PersistenceExtension;
import org.gluu.service.custom.script.ExternalScriptService;
import org.gluu.service.external.context.PersistenceExternalContext;

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
		PersistenceExtension persistenceExtension = null;
		if (isEnabled()) {
			persistenceExtension = (PersistenceExtension) this.defaultExternalCustomScript.getExternalType();
		}
		
		for (Iterator<PersistenceEntryManager> it = persistenceEntryManagerInstance.iterator(); it.hasNext();) {
			PersistenceEntryManager persistenceEntryManager = it.next();
			persistenceEntryManager.setPersistenceExtension(persistenceExtension);
		}

		for (Iterator<List<PersistenceEntryManager>> it = persistenceEntryManagerListInstance.iterator(); it.hasNext();) {
			List<PersistenceEntryManager> persistenceEntryManagerList = it.next();
			for (PersistenceEntryManager persistenceEntryManager: persistenceEntryManagerList) {
				persistenceEntryManager.setPersistenceExtension(persistenceExtension);
			}
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
