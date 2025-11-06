package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.TxTokenType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.List;

@ApplicationScoped
public class ExternalTxTokenService extends ExternalScriptService {

    public ExternalTxTokenService() {
        super(CustomScriptType.TX_TOKEN);
    }

    public boolean modifyTokenPayload(CustomScriptConfiguration script, JsonWebResponse jsonWebResponse, ExternalScriptContext context) {
        try {
            log.trace("Executing 'modifyTokenPayload' method, script name: {}, jsonWebResponse: {}, context: {}", script.getName(), jsonWebResponse, context);
            context.getExecutionContext().setScript(script);

            TxTokenType scriptType = (TxTokenType) script.getExternalType();
            final boolean result = scriptType.modifyTokenPayload(jsonWebResponse, context);
            log.trace("Finished 'modifyTokenPayload' method, script name: {}, jsonWebResponse: {}, context: {}, result: {}", script.getName(), jsonWebResponse, context, result);

            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean modifyTokenPayload(JsonWebResponse jsonWebResponse, ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context.getExecutionContext());
        if (scripts.isEmpty()) {
            return true;
        }
        log.trace("Executing {} 'modifyTokenPayload' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyTokenPayload(script, jsonWebResponse, context)) {
                return false;
            }
        }

        return true;
    }

    public boolean modifyResponse(CustomScriptConfiguration script, JSONObject response, ExternalScriptContext context) {
        try {
            log.trace("Executing 'modifyResponse' method, script name: {}, response: {}, context: {}", script.getName(), response, context);
            context.getExecutionContext().setScript(script);

            TxTokenType scriptType = (TxTokenType) script.getExternalType();
            final boolean result = scriptType.modifyResponse(response, context);
            log.trace("Finished 'modifyResponse' method, script name: {}, response: {}, context: {}, result: {}", script.getName(), response, context, result);

            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean modifyResponse(JSONObject response, ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context.getExecutionContext());
        if (scripts.isEmpty()) {
            log.trace("No TxTokenType scripts found.");
            return true;
        }

        log.trace("Executing {} 'modifyResponse' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyResponse(script, response, context)) {
                return false;
            }
        }

        return true;
    }

    public int getTxTokenLifetimeInSeconds(CustomScriptConfiguration script, ExternalScriptContext context) {
        try {
            log.trace("Executing 'getTxTokenLifetimeInSeconds' method, script name: {}, context: {}", script.getName(), context);
            context.getExecutionContext().setScript(script);

            TxTokenType txTokenType = (TxTokenType) script.getExternalType();
            final int result = txTokenType.getTxTokenLifetimeInSeconds(context);
            log.trace("Finished 'getTxTokenLifetimeInSeconds' method, script name: {}, context: {}, result: {}", script.getName(), context, result);

            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }
        return 0;
    }

    public int getTxTokenLifetimeInSeconds(ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context.getExecutionContext());
        if (scripts.isEmpty()) {
            return 0;
        }
        log.trace("Executing {} 'getTxTokenLifetimeInSeconds' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final int lifetime = getTxTokenLifetimeInSeconds(script, context);
            if (lifetime > 0) {
                log.trace("Finished 'getTxTokenLifetimeInSeconds' methods, lifetime: {}", lifetime);
                return lifetime;
            }
        }
        return 0;
    }

    @NotNull
    private List<CustomScriptConfiguration> getScripts(@NotNull ExecutionContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty() || context.getClient() == null) {
            log.trace("No TxToken scripts or client is null.");
            return Lists.newArrayList();
        }

        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(context.getClient().getAttributes().getTxTokenScriptDns());
        if (!scripts.isEmpty()) {
            return scripts;
        }

        log.trace("No TxToken scripts associated with client {}", context.getClient().getClientId());
        return Lists.newArrayList();
    }
}
