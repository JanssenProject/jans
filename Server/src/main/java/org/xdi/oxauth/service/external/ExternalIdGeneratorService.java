/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.Map;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.id.IdGeneratorType;
import org.xdi.service.custom.script.ExternalScriptService;

/**
 * Provides factory methods needed to create external id generator extension
 * 
 * @author Yuriy Movchan Date: 01/16/2015
 */
@Scope(ScopeType.APPLICATION)
@Name("externalIdGeneratorService")
@AutoCreate
@Startup
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
		}

		return null;
	}

	public String executeExternalDefaultGenerateIdMethod(String appId, String idType, String idPrefix) {
		return executeExternalGenerateIdMethod(this.defaultExternalCustomScript, appId, idType, idPrefix);
	}

	public static ExternalIdGeneratorService instance() {
		return (ExternalIdGeneratorService) Component.getInstance(ExternalIdGeneratorService.class);
	}

}
