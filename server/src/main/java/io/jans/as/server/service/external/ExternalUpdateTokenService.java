/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.UpdateTokenType;
import io.jans.service.custom.script.ExternalScriptService;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

/**
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class ExternalUpdateTokenService extends ExternalScriptService {

    private static final long serialVersionUID = -1033475075863270259L;

    public ExternalUpdateTokenService() {
        super(CustomScriptType.UPDATE_TOKEN);
    }

    public boolean modifyIdTokenMethod(CustomScriptConfiguration script, JsonWebResponse jsonWebResponse, ExternalUpdateTokenContext context) {
        try {
            log.trace("Executing python 'updateToken' method, script name: {}, jsonWebResponse: {}, context: {}", script.getName(), jsonWebResponse, context);
            context.setScript(script);

            UpdateTokenType updateTokenType = (UpdateTokenType) script.getExternalType();
            final boolean result = updateTokenType.modifyIdToken(jsonWebResponse, context);
            log.trace("Finished 'updateToken' method, script name: {}, jsonWebResponse: {}, context: {}, result: {}", script.getName(), jsonWebResponse, context, result);

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean modifyIdTokenMethods(JsonWebResponse jsonWebResponse, ExternalUpdateTokenContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Executing {} update-token scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyIdTokenMethod(script, jsonWebResponse, context)) {
                return false;
            }
        }

        return true;
    }

    public Function<JsonWebResponse, Void> buildModifyIdTokenProcessor(final ExternalUpdateTokenContext context) {
        return jsonWebResponse -> {
            modifyIdTokenMethods(jsonWebResponse, context);

            return null;
        };
    }

    public int getRefreshTokenLifetimeInSeconds(CustomScriptConfiguration script, ExternalUpdateTokenContext context) {
        try {
            log.trace("Executing python 'getRefreshTokenLifetimeInSeconds' method, script name: {}, context: {}", script.getName(), context);
            context.setScript(script);

            UpdateTokenType updateTokenType = (UpdateTokenType) script.getExternalType();
            final int result = updateTokenType.getRefreshTokenLifetimeInSeconds(context);
            log.trace("Finished 'getRefreshTokenLifetimeInSeconds' method, script name: {}, context: {}, result: {}", script.getName(), context, result);

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }
        return 0;
    }

    public int getRefreshTokenLifetimeInSeconds(ExternalUpdateTokenContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return 0;
        }
        log.trace("Executing {} 'getRefreshTokenLifetimeInSeconds' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final int lifetime = getRefreshTokenLifetimeInSeconds(script, context);
            if (lifetime > 0) {
                log.trace("Finished 'getRefreshTokenLifetimeInSeconds' methods, lifetime: {}", lifetime);
                return lifetime;
            }
        }
        return 0;
    }

    @NotNull
    private List<CustomScriptConfiguration> getScripts(@NotNull ExternalUpdateTokenContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty() || context.getClient() == null) {
            return Lists.newArrayList();
        }

        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(context.getClient().getAttributes().getUpdateTokenScriptDns());
        if (!scripts.isEmpty()) {
            return scripts;
        }

        log.trace("No UpdateToken scripts associated with client {}", context.getClient().getClientId());
        return Lists.newArrayList();
    }

    public boolean modifyRefreshToken(CustomScriptConfiguration script, RefreshToken refreshToken, ExternalUpdateTokenContext context) {
        try {
            log.trace("Executing python 'modifyRefreshToken' method, script name: {}, context: {}", script.getName(), context);
            context.setScript(script);

            UpdateTokenType updateTokenType = (UpdateTokenType) script.getExternalType();
            final boolean result = updateTokenType.modifyRefreshToken(refreshToken, context);
            log.trace("Finished 'modifyRefreshToken' method, script name: {}, context: {}, result: {}", script.getName(), context, result);

            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean modifyRefreshToken(RefreshToken refreshToken, ExternalUpdateTokenContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Executing {} update-token modifyRefreshToken scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyRefreshToken(script, refreshToken, context)) {
                return false;
            }
        }

        return true;
    }
}
