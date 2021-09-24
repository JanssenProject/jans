/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import java.util.List;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.json.JSONObject;

import io.jans.as.server.service.external.context.ExternalUmaRptClaimsContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.uma.UmaRptClaimsType;
import io.jans.service.custom.script.ExternalScriptService;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalUmaRptClaimsService extends ExternalScriptService {

    public ExternalUmaRptClaimsService() {
        super(CustomScriptType.UMA_RPT_CLAIMS);
    }

    public boolean externalModify(JSONObject rptAsJson, ExternalUmaRptClaimsContext context) {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(context.getClient().getAttributes().getRptClaimsScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} RPT Claims scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalModify(rptAsJson, script, context)) {
                return false;
            }
        }

        log.debug("ExternalModify returned 'true'.");
        return true;
    }

    public boolean externalModify(JSONObject rptAsJson, CustomScriptConfiguration scriptConfiguration, ExternalUmaRptClaimsContext context) {
        try {
            log.trace("Executing external 'externalModify' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            UmaRptClaimsType script = (UmaRptClaimsType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.modify(rptAsJson, context);

            log.trace("Finished external 'externalModify' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }
}
