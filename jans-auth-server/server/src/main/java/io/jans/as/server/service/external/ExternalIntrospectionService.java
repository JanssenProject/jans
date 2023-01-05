/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.introspection.IntrospectionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalIntrospectionService extends ExternalScriptService {

    private static final long serialVersionUID = -8609727759114795446L;

    @Inject
    private AppConfiguration appConfiguration;

    public ExternalIntrospectionService() {
        super(CustomScriptType.INTROSPECTION);
    }

    @NotNull
    private List<CustomScriptConfiguration> getScripts(@NotNull ExternalIntrospectionContext context) {
        if (customScriptConfigurations == null) {
            return Lists.newArrayList();
        }
        if (appConfiguration.getIntrospectionScriptBackwardCompatibility()) {
            return customScriptConfigurations;
        }

        if (context.getGrantOfIntrospectionToken() != null && context.getGrantOfIntrospectionToken().getClient() != null) {
            final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(context.getGrantOfIntrospectionToken().getClient().getAttributes().getIntrospectionScripts());
            if (!scripts.isEmpty()) {
                return scripts;
            }
        }

        if (context.getTokenGrant() != null && context.getTokenGrant().getClient() != null) { // fallback to authorization grant
            final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(context.getTokenGrant().getClient().getAttributes().getIntrospectionScripts());
            if (!scripts.isEmpty()) {
                return scripts;
            }
        }

        log.trace("No introspection scripts associated with client which was used to obtain access_token.");
        return Lists.newArrayList();
    }

    public boolean executeExternalModifyResponse(JSONObject responseAsJsonObject, ExternalIntrospectionContext context) {
        final List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            log.trace("There is no any external interception scripts defined.");
            return false;
        }

        for (CustomScriptConfiguration script : scripts) {
            if (!executeExternalModifyResponse(script, responseAsJsonObject, context)) {
                log.debug("Stopped running external interception scripts because script {} returns false.", script.getName());
                return false;
            }
        }

        return true;
    }

    private boolean executeExternalModifyResponse(CustomScriptConfiguration scriptConf, JSONObject responseAsJsonObject, ExternalIntrospectionContext context) {
        try {
            log.trace("Executing external 'executeExternalModifyResponse' method, script name: {}, responseAsJsonObject: {} , context: {}",
                    scriptConf.getName(), responseAsJsonObject, context);

            IntrospectionType script = (IntrospectionType) scriptConf.getExternalType();
            context.setScript(scriptConf);
            final boolean result = script.modifyResponse(responseAsJsonObject, context);
            log.trace("Finished external 'executeExternalModifyResponse' method, script name: {}, responseAsJsonObject: {} , context: {}, result: {}",
                    scriptConf.getName(), responseAsJsonObject, context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConf.getCustomScript(), ex);
            return false;
        }
    }
}
