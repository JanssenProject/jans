/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script.test;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.idp.IdpType;
import io.jans.service.custom.script.ExternalScriptService;

/**
 * External IDP script service
 *
 * @author Yuriy Movchan
 * @version 0.1, 06/18/2020
 */
public class SampleIdpExternalScriptService extends ExternalScriptService {

	private static final long serialVersionUID = -1316361273036208685L;

	public SampleIdpExternalScriptService() {
		super(CustomScriptType.IDP);
	}

    //  boolean translateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean executeExternalTranslateAttributesMethod(Object context, CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'translateAttributes' method");
			IdpType idpType = (IdpType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return idpType.translateAttributes(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}

		return false;
	}

	public boolean executeExternalTranslateAttributesMethod(Object context) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			if (customScriptConfiguration.getExternalType().getApiVersion() > 1) {
				result &= executeExternalTranslateAttributesMethod(context, customScriptConfiguration);
				if (!result) {
					return result;
				}
			}
		}

		return result;
	}

    //  boolean updateAttributes(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean executeExternalUpdateAttributesMethod(Object context, CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'updateAttributes' method");
			IdpType idpType = (IdpType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return idpType.updateAttributes(context, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			saveScriptError(customScriptConfiguration.getCustomScript(), ex);
		}

		return false;
	}

	public boolean executeExternalUpdateAttributesMethods(Object context) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			if (customScriptConfiguration.getExternalType().getApiVersion() > 1) {
				result &= executeExternalUpdateAttributesMethod(context, customScriptConfiguration);
				if (!result) {
					return result;
				}
			}
		}

		return result;
	}

}
