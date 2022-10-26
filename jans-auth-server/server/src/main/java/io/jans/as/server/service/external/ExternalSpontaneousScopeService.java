/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.server.service.external.context.SpontaneousScopeExternalContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.spontaneous.SpontaneousScopeType;
import io.jans.service.custom.script.ExternalScriptService;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalSpontaneousScopeService extends ExternalScriptService {

    public ExternalSpontaneousScopeService() {
        super(CustomScriptType.SPONTANEOUS_SCOPE);
    }

    public void executeExternalManipulateScope(SpontaneousScopeExternalContext context) {
        for (CustomScriptConfiguration script : getScriptsToExecute(context.getClient())) {
            executeExternalManipulateScope(script, context);

            log.debug("GrantedScopes {} after execution of interception script {}.", context.getGrantedScopes(), script.getName());
        }
    }

    private void executeExternalManipulateScope(CustomScriptConfiguration scriptConfiguration, SpontaneousScopeExternalContext context) {
        try {
            log.debug("Executing external 'executeExternalManipulateScope' method, script name: {}, grantedScopes: {} , context: {}",
                    scriptConfiguration.getName(), context.getGrantedScopes(), context);

            SpontaneousScopeType script = (SpontaneousScopeType) scriptConfiguration.getExternalType();

            script.manipulateScopes(context);
            log.debug("Finished external 'executeExternalManipulateScope' method, script name: {}, grantedScopes: {} , context: {}",
                    scriptConfiguration.getName(), context.getGrantedScopes(), context);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
        }
    }

    private Set<CustomScriptConfiguration> getScriptsToExecute(Client client) {
        Set<CustomScriptConfiguration> result = Sets.newHashSet();
        if (this.customScriptConfigurations == null) {
            return result;
        }

        List<String> scriptDns = client.getAttributes().getSpontaneousScopeScriptDns();
        for (CustomScriptConfiguration script : this.customScriptConfigurations) {
            if (scriptDns.contains(script.getCustomScript().getDn())) {
                result.add(script);
            }
        }
        return result;
    }
}
