/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.ssa.ModifySsaResponseType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ModifySsaResponseService extends ExternalScriptService {

    private static final long serialVersionUID = -1033475075863270259L;

    public ModifySsaResponseService() {
        super(CustomScriptType.MODIFY_SSA_RESPONSE);
    }

    public boolean create(CustomScriptConfiguration script, JsonWebResponse jsonWebResponse, ModifySsaResponseContext context) {
        try {
            log.trace("Executing python modify-ssa-response method, script name: {}, jwt: {}, context: {}", script.getName(), jsonWebResponse, context);
            context.setScript(script);

            ModifySsaResponseType modifySsaResponseType = (ModifySsaResponseType) script.getExternalType();
            final boolean result = modifySsaResponseType.create(jsonWebResponse, context);
            log.trace("Finished modify-ssa-response method, script name: {}, jwt: {}, context: {}, result: {}", script.getName(), jsonWebResponse, context, result);

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean create(JsonWebResponse jsonWebResponse, ModifySsaResponseContext context) {
        List<CustomScriptConfiguration> scripts = customScriptManager.getCustomScriptConfigurationsByScriptType(CustomScriptType.MODIFY_SSA_RESPONSE);
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Executing {} modify-ssa-response scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!create(script, jsonWebResponse, context)) {
                return false;
            }
        }

        return true;
    }

    public Function<JsonWebResponse, Void> buildCreateProcessor(final ModifySsaResponseContext context) {
        return jsonWebResponse -> {
            create(jsonWebResponse, context);
            return null;
        };
    }
}
