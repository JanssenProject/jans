/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.service.external;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.persistence.PersistenceType;
import org.gluu.service.custom.script.ExternalScriptService;
import org.gluu.service.external.context.PersistenceExternalContext;

/**
 * Provides factory methods needed to create persistence extension
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
@ApplicationScoped
public class ExternalPersistenceExtension extends ExternalScriptService {

	private static final long serialVersionUID = 5466361778036208685L;

	public ExternalPersistenceExtension() {
		super(CustomScriptType.PERSISTENCE_EXTENSION);
	}

	public void executeExternalOnBeforeCreateMethod(CustomScriptConfiguration customScriptConfiguration, PersistenceExternalContext context) {
		try {
			log.debug("Executing python 'onBeforeCreate' method");
			PersistenceType persistenceType = (PersistenceType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			persistenceType.onBeforeCreate(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}
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

	public void executeExternalOnBeforeDestroyMethod(CustomScriptConfiguration customScriptConfiguration, PersistenceExternalContext context) {
		try {
			log.debug("Executing python 'onBeforeDestroy' method");
			PersistenceType persistenceType = (PersistenceType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			persistenceType.onBeforeDestroy(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}
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
