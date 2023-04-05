/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import java.util.Map;

//import javax.inject.Named;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.id.IdGeneratorType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Provides factory methods needed to create external id generator extension
 * 
 * @author Yuriy Movchan Date: 01/16/2015
 */
@ApplicationScoped
@Named("externalIdGeneratorService")
public class ExternalIdGeneratorService extends ExternalScriptService {

	private static final long serialVersionUID = 1727751544454591273L;

	public ExternalIdGeneratorService() {
		super(CustomScriptType.ID_GENERATOR);
	}

	public String executeExternalGenerateIdMethod(CustomScriptConfiguration customScriptConfiguration, String appId, String idType, String idPrefix) {
		try {
			log.debug("Executing python 'generateId' method");
			IdGeneratorType externalType = (IdGeneratorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalType.generateId(appId, idType, idPrefix, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}

		return null;
	}

	public String executeExternalDefaultGenerateIdMethod(String appId, String idType, String idPrefix) {
		return executeExternalGenerateIdMethod(this.defaultExternalCustomScript, appId, idType, idPrefix);
	}

}
