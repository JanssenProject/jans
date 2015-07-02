/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.List;
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
import org.xdi.model.custom.script.type.scope.DynamicScopeType;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.service.external.context.DynamicScopeExternalContext;
import org.xdi.service.custom.script.ExternalScriptService;

/**
 * Provides factory methods needed to create dynamic scope extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
@Scope(ScopeType.APPLICATION)
@Name("externalDynamicScopeService")
@AutoCreate
@Startup
public class ExternalDynamicScopeService extends ExternalScriptService {

	private static final long serialVersionUID = 1416361273036208685L;

	public ExternalDynamicScopeService() {
		super(CustomScriptType.DYNAMIC_SCOPE);
	}

	public boolean executeExternalUpdateMethod(CustomScriptConfiguration customScriptConfiguration, DynamicScopeExternalContext dynamicScopeContext) {
		try {
			log.debug("Executing python 'update' method");
			DynamicScopeType dynamicScopeType = (DynamicScopeType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return dynamicScopeType.update(dynamicScopeContext, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeExternalUpdateMethods(List<String> dynamicScopes, JsonWebResponse jsonToken, User user) {
		DynamicScopeExternalContext dynamicScopeContext = new DynamicScopeExternalContext(dynamicScopes, jsonToken, user);

		return executeExternalUpdateMethods(dynamicScopeContext);
	}

	public boolean executeExternalUpdateMethods(DynamicScopeExternalContext dynamicScopeContext) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			result &= executeExternalUpdateMethod(customScriptConfiguration, dynamicScopeContext);
			if (!result) {
				return result;
			}
		}

		return result;
	}

    public static ExternalDynamicScopeService instance() {
        return (ExternalDynamicScopeService) Component.getInstance(ExternalDynamicScopeService.class);
    }

}
