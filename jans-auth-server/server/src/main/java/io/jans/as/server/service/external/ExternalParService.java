package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.par.ParType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalParService extends ExternalScriptService {

    public ExternalParService() {
        super(CustomScriptType.PAR);
    }

    public boolean createPar(CustomScriptConfiguration script, Par par, ExternalScriptContext context) {
        try {
            log.trace("Executing 'createPar' method, script name: {}, par: {}, context: {}", script.getName(), par, context);
            context.getExecutionContext().setScript(script);

            ParType scriptType = (ParType) script.getExternalType();
            final boolean result = scriptType.createPar(par, context);
            log.trace("Finished 'createPar' method, script name: {}, par: {}, context: {}, result: {}", script.getName(), par, context, result);

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

    public boolean createPar(Par par, ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Executing {} 'createPar' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!createPar(script, par, context)) {
                return false;
            }
        }

        return true;
    }

    public boolean modifyParResponse(CustomScriptConfiguration script, JSONObject response, ExternalScriptContext context) {
        try {
            log.trace("Executing 'modifyParResponse' method, script name: {}, response: {}, context: {}", script.getName(), response, context);
            context.getExecutionContext().setScript(script);

            ParType scriptType = (ParType) script.getExternalType();
            final boolean result = scriptType.modifyParResponse(response, context);
            log.trace("Finished 'modifyParResponse' method, script name: {}, response: {}, context: {}, result: {}", script.getName(), response, context, result);

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

    public boolean modifyParResponse(JSONObject response, ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return false;
        }

        log.trace("Executing {} 'modifyParResponse' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyParResponse(script, response, context)) {
                return false;
            }
        }

        return true;
    }


    @NotNull
    private List<CustomScriptConfiguration> getScripts(@NotNull ExternalScriptContext context) {
        final Client client = context.getExecutionContext().getClient();
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty() || client == null) {
            log.trace("No ParType scripts or client is null.");
            return Lists.newArrayList();
        }

        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getParScriptDns());
        if (!scripts.isEmpty()) {
            return scripts;
        }

        log.trace("No ParType scripts associated with client {}", client.getClientId());
        return Lists.newArrayList();
    }
}
