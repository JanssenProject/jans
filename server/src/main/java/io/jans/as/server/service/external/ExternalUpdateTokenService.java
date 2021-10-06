/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.UpdateTokenType;
import io.jans.service.custom.script.ExternalScriptService;

import javax.enterprise.context.ApplicationScoped;
import java.util.function.Function;

/**
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class ExternalUpdateTokenService extends ExternalScriptService {

	private static final long serialVersionUID = -1033475075863270249L;

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
        if (this.customScriptConfigurations.isEmpty()) {
            return false;
        }
        log.trace("Executing {} update-token scripts.", this.customScriptConfigurations.size());

        for (CustomScriptConfiguration script : this.customScriptConfigurations) {
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
        if (this.customScriptConfigurations.isEmpty()) {
            return 0;
        }
        log.trace("Executing {} 'getRefreshTokenLifetimeInSeconds' scripts.", this.customScriptConfigurations.size());

        for (CustomScriptConfiguration script : this.customScriptConfigurations) {
            final int lifetime = getRefreshTokenLifetimeInSeconds(script, context);
            if (lifetime > 0) {
                log.trace("Finished 'getRefreshTokenLifetimeInSeconds' methods, lifetime: {}", lifetime);
                return lifetime;
            }
        }
        return 0;
    }

}
