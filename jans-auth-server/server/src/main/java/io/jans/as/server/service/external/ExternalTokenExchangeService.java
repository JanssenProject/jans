package io.jans.as.server.service.external;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.ScriptTokenExchangeControl;
import io.jans.model.custom.script.type.token.TokenExchangeType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalTokenExchangeService extends ExternalScriptService {

    public ExternalTokenExchangeService() {
        super(CustomScriptType.TOKEN_EXCHANGE);
    }

    public ScriptTokenExchangeControl externalValidate(ExecutionContext context) {
        final Client client = context.getClient();
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return ScriptTokenExchangeControl.fail();
        }
        log.trace("Found {} token-exchange scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final ScriptTokenExchangeControl control = externalValidate(script, context);
            if (control != null) {
                return control;
            }
        }

        return null;
    }

    public ScriptTokenExchangeControl externalValidate(CustomScriptConfiguration script, ExecutionContext context) {
        final Client client = context.getClient();
        log.trace("Executing external 'validate' method, script name: {}, clientId: {}",
                script.getName(), client.getClientId());

        ScriptTokenExchangeControl result = null;
        try {
            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            TokenExchangeType tokenExchangeType = (TokenExchangeType) script.getExternalType();
            result = tokenExchangeType.validate(scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        log.trace("Finished 'validate' method, script name: {}, clientId: {}, result: {}", script.getName(), client.getClientId(), result);

        return null;
    }

    public boolean externalModifyResponse(JSONObject response, ExecutionContext context) {
        final Client client = context.getClient();
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} token-exchange scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalModifyResponse(response, script, context)) {
                return false;
            }
        }

        return true;
    }

    public boolean externalModifyResponse(JSONObject response, CustomScriptConfiguration script, ExecutionContext context) {
        final Client client = context.getClient();

        log.debug("Executing external 'modifyResponse' method, script name: {}, clientId: {}, response: {}",
                script.getName(), client.getClientId(), response);

        boolean result = false;
        try {
            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            TokenExchangeType tokenExchangeType = (TokenExchangeType) script.getExternalType();
            result = tokenExchangeType.modifyResponse(response, scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        log.debug("Finished 'modifyResponse' method, script name: {}, clientId: {}, result: {}, response: {}", script.getName(), client.getClientId(), result, response);

        return result;
    }
}
