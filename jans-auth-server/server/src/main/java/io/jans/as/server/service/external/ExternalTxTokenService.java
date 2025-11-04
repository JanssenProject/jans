package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.TxTokenType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ApplicationScoped
public class ExternalTxTokenService extends ExternalScriptService {

    public ExternalTxTokenService() {
        super(CustomScriptType.TX_TOKEN);
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
