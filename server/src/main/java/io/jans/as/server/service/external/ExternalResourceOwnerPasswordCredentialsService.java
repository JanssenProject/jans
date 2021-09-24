/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.jans.as.server.service.external.context.ExternalResourceOwnerPasswordCredentialsContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import io.jans.service.custom.script.ExternalScriptService;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalResourceOwnerPasswordCredentialsService extends ExternalScriptService {

    private static final long serialVersionUID = -1070021905117551202L;

    public ExternalResourceOwnerPasswordCredentialsService() {
        super(CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
    }

    public boolean executeExternalAuthenticate(ExternalResourceOwnerPasswordCredentialsContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.debug("There is no any external interception scripts defined.");
            return false;
        }

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!executeExternalAuthenticate(script, context)) {
                log.debug("Stopped running external RO PC scripts because script {} returns false.", script.getName());
                return false;
            }
        }

        return true;
    }

    private boolean executeExternalAuthenticate(CustomScriptConfiguration customScriptConfiguration, ExternalResourceOwnerPasswordCredentialsContext context) {
        try {
            log.debug("Executing external 'executeExternalAuthenticate' method, script name: {}, context: {}",
                    customScriptConfiguration.getName(), context);

            ResourceOwnerPasswordCredentialsType script = (ResourceOwnerPasswordCredentialsType) customScriptConfiguration.getExternalType();
            context.setScript(customScriptConfiguration);

            if (script == null) {
                log.error("Failed to load script, name: " + customScriptConfiguration.getName());
                return false;
            }

            final boolean result = script.authenticate(context);

            log.debug("Finished external 'executeExternalAuthenticate' method, script name: {}, context: {}, result: {}",
                    customScriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

}
