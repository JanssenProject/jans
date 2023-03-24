/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.ssa.ModifySsaResponseType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

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
            log.trace("Executing python modify-ssa-response method create, script name: {}, jwt: {}, context: {}", script.getName(), jsonWebResponse, context);
            context.setScript(script);

            ModifySsaResponseType modifySsaResponseType = (ModifySsaResponseType) script.getExternalType();
            final boolean result = modifySsaResponseType.create(jsonWebResponse, context);
            log.trace("Finished modify-ssa-response method create, script name: {}, jwt: {}, context: {}, result: {}", script.getName(), jsonWebResponse, context, result);

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean create(JsonWebResponse jsonWebResponse, ModifySsaResponseContext context) {
        List<CustomScriptConfiguration> scripts = getCustomScript();
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

    public boolean get(JSONArray jsonArray, ModifySsaResponseContext context) {
        List<CustomScriptConfiguration> scriptList = getCustomScript();
        if (scriptList.isEmpty()) {
            return false;
        }
        for (CustomScriptConfiguration script : scriptList) {
            log.trace("Executing python modify-ssa-response method get, script name: {}, jsonArray: {}, context: {}", script.getName(), jsonArray, context);
            context.setScript(script);

            ModifySsaResponseType modifySsaResponseType = (ModifySsaResponseType) script.getExternalType();
            boolean result = false;
            try {
                result = modifySsaResponseType.get(jsonArray, context);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                saveScriptError(script.getCustomScript(), e);
            }
            log.trace("Finished modify-ssa-response method get, script name: {}, jsonArray: {}, context: {}, result: {}", script.getName(), jsonArray, context, result);
            if (!result) {
                return false;
            }
        }
        return true;
    }

    public boolean revoke(List<Ssa> ssaList, ModifySsaResponseContext context) {
        List<CustomScriptConfiguration> scriptList = getCustomScript();
        if (scriptList.isEmpty()) {
            return false;
        }
        for (CustomScriptConfiguration script : scriptList) {
            log.trace("Executing python modify-ssa-response method revoke, script name: {}, ssaList: {}, context: {}", script.getName(), ssaList, context);
            context.setScript(script);

            ModifySsaResponseType modifySsaResponseType = (ModifySsaResponseType) script.getExternalType();
            boolean result = false;
            try {
                result = modifySsaResponseType.revoke(ssaList, context);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                saveScriptError(script.getCustomScript(), e);
            }
            log.trace("Finished modify-ssa-response method revoke, script name: {}, ssaList: {}, context: {}, result: {}", script.getName(), ssaList, context, result);
            if (!result) {
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

    @NotNull
    private List<CustomScriptConfiguration> getCustomScript() {
        return customScriptManager.getCustomScriptConfigurationsByScriptType(CustomScriptType.MODIFY_SSA_RESPONSE);
    }
}
