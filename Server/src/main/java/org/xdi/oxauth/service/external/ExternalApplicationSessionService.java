/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.session.ApplicationSessionType;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.service.custom.script.ExternalScriptService;

/**
 * Provides factory methods needed to create external application session extension
 *
 * @author Yuriy Movchan Date: 01/20/2015
 */
@Scope(ScopeType.APPLICATION)
@Name("externalApplicationSessionService")
@AutoCreate
@Startup
public class ExternalApplicationSessionService extends ExternalScriptService {

	private static final long serialVersionUID = 2316361273036208685L;

	public ExternalApplicationSessionService() {
		super(CustomScriptType.APPLICATION_SESSION);
	}

	public boolean executeExternalEndSessionMethod(CustomScriptConfiguration customScriptConfiguration, HttpServletRequest httpRequest, SessionState sessionState) {
		try {
			log.debug("Executing python 'endSession' method");
			ApplicationSessionType applicationSessionType = (ApplicationSessionType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return applicationSessionType.endSession(httpRequest, sessionState, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeExternalEndSessionMethods(HttpServletRequest httpRequest, SessionState sessionState) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			result &= executeExternalEndSessionMethod(customScriptConfiguration, httpRequest, sessionState);
			if (!result) {
				return result;
			}
		}

		return result;
	}

    public static ExternalApplicationSessionService instance() {
        return (ExternalApplicationSessionService) Component.getInstance(ExternalApplicationSessionService.class);
    }

}
