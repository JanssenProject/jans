/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.server.service.external.context.ExternalCibaEndUserNotificationContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.ciba.EndUserNotificationType;
import io.jans.service.custom.script.ExternalScriptService;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Milton BO
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalCibaEndUserNotificationService extends ExternalScriptService {

    private static final long serialVersionUID = -8609727759114795446L;

    public ExternalCibaEndUserNotificationService() {
        super(CustomScriptType.CIBA_END_USER_NOTIFICATION);
    }

    public boolean executeExternalNotifyEndUser(ExternalCibaEndUserNotificationContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.trace("There is no any external interception scripts defined.");
            return false;
        }

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!executeExternalNotifyEndUser(script, context)) {
                log.trace("Stopped running external interception scripts because script {} returns false.", script.getName());
                return false;
            }
        }
        return true;
    }

    private boolean executeExternalNotifyEndUser(CustomScriptConfiguration customScriptConfiguration,
                                                 ExternalCibaEndUserNotificationContext context) {
        try {
            log.trace("Executing external 'executeExternalNotifyEndUser' method, script name: {}, context: {}",
                    customScriptConfiguration.getName(), context);

            EndUserNotificationType script = (EndUserNotificationType) customScriptConfiguration.getExternalType();
            final boolean result = script.notifyEndUser(context);
            log.trace("Finished external 'executeExternalNotifyEndUser' method, script name: {}, context: {}, result: {}",
                    customScriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }
}
