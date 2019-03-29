/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.Map;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.session.ApplicationSessionType;
import org.gluu.service.custom.script.ExternalScriptService;
import org.xdi.oxauth.model.common.SessionId;

/**
 * Provides factory methods needed to create external application session extension
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalApplicationSessionService extends ExternalScriptService {

    private static final long serialVersionUID = 2316361273036208685L;

    public ExternalApplicationSessionService() {
        super(CustomScriptType.APPLICATION_SESSION);
    }

    public boolean executeExternalStartSessionMethod(CustomScriptConfiguration customScriptConfiguration, HttpServletRequest httpRequest, SessionId sessionId) {
        try {
            log.debug("Executing python 'startSession' method");
            ApplicationSessionType applicationSessionType = (ApplicationSessionType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return applicationSessionType.startSession(httpRequest, sessionId, configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public boolean executeExternalStartSessionMethods(HttpServletRequest httpRequest, SessionId sessionId) {
        boolean result = true;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (customScriptConfiguration.getExternalType().getApiVersion() > 1) {
                result &= executeExternalStartSessionMethod(customScriptConfiguration, httpRequest, sessionId);
                if (!result) {
                    return result;
                }
            }
        }

        return result;
    }

    public boolean executeExternalEndSessionMethod(CustomScriptConfiguration customScriptConfiguration, HttpServletRequest httpRequest, SessionId sessionId) {
        try {
            log.debug("Executing python 'endSession' method");
            ApplicationSessionType applicationSessionType = (ApplicationSessionType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return applicationSessionType.endSession(httpRequest, sessionId, configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public boolean executeExternalEndSessionMethods(HttpServletRequest httpRequest, SessionId sessionId) {
        boolean result = true;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            result &= executeExternalEndSessionMethod(customScriptConfiguration, httpRequest, sessionId);
            if (!result) {
                return result;
            }
        }

        return result;
    }

}
