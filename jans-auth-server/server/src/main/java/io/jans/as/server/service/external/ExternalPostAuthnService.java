/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.service.external.context.ExternalPostAuthnContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.service.custom.script.ExternalScriptService;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalPostAuthnService extends ExternalScriptService {

    public ExternalPostAuthnService() {
        super(CustomScriptType.POST_AUTHN);
    }

    public boolean externalForceReAuthentication(Client client, ExternalPostAuthnContext context) {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} post-authn scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalForceReAuthentication(script, context)) {
                return false;
            }
        }

        log.debug("Forcing re-authentication via post-authn script.");
        return true;
    }

    public boolean externalForceAuthorization(Client client, ExternalPostAuthnContext context) {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} post-authn scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalForceAuthorization(script, context)) {
                return false;
            }
        }

        log.debug("Forcing authorization via post-authn script.");
        return true;
    }


    public boolean externalForceReAuthentication(CustomScriptConfiguration scriptConfiguration, ExternalPostAuthnContext context) {
        try {
            log.trace("Executing external 'externalForceReAuthentication' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            PostAuthnType script = (PostAuthnType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.forceReAuthentication(context);

            log.trace("Finished external 'externalForceReAuthentication' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public boolean externalForceAuthorization(CustomScriptConfiguration scriptConfiguration, ExternalPostAuthnContext context) {
        try {
            log.trace("Executing external 'externalForceAuthorization' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            PostAuthnType script = (PostAuthnType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.forceAuthorization(context);

            log.trace("Finished external 'externalForceAuthorization' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }
}
