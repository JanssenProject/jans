package io.jans.as.server.service.external;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.LogoutStatusJwtType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalLogoutStatusJwtService extends ExternalScriptService {

    public ExternalLogoutStatusJwtService() {
        super(CustomScriptType.LOGOUT_STATUS_JWT);
    }

    public boolean modifyLogoutStatusJwtMethod(CustomScriptConfiguration script, JsonWebResponse jsonWebResponse, ExternalScriptContext context) {
        try {
            log.trace("Executing 'modifyLogoutStatusJwtMethod' method, script name: {}, jsonWebResponse: {}, context: {}", script.getName(), jsonWebResponse, context);
            context.getExecutionContext().setScript(script);

            LogoutStatusJwtType scriptType = (LogoutStatusJwtType) script.getExternalType();
            final boolean result = scriptType.modifyPayload(jsonWebResponse, context);
            log.trace("Finished 'modifyLogoutStatusJwtMethod' method, script name: {}, jsonWebResponse: {}, context: {}, result: {}", script.getName(), jsonWebResponse, context, result);

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

    public boolean modifyLogoutStatusJwtMethod(JsonWebResponse jsonWebResponse, ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Executing {} 'modifyLogoutStatusJwtMethod' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!modifyLogoutStatusJwtMethod(script, jsonWebResponse, context)) {
                return false;
            }
        }

        return true;
    }

    public int getLifetimeInSeconds(CustomScriptConfiguration script, ExternalScriptContext context) {
        try {
            log.trace("Executing python 'getLifetimeInSeconds' method, script name: {}, context: {}", script.getName(), context);
            context.getExecutionContext().setScript(script);

            LogoutStatusJwtType scriptType = (LogoutStatusJwtType) script.getExternalType();
            final int result = scriptType.getLifetimeInSeconds(context);
            log.trace("Finished 'getLifetimeInSeconds' method, script name: {}, context: {}, result: {}", script.getName(), context, result);

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

    public int getLifetimeInSeconds(ExternalScriptContext context) {
        List<CustomScriptConfiguration> scripts = getScripts(context);
        if (scripts.isEmpty()) {
            return 0;
        }
        log.trace("Executing {} 'getLifetimeInSeconds' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final int lifetime = getLifetimeInSeconds(script, context);
            if (lifetime > 0) {
                log.trace("Finished 'getLifetimeInSeconds' methods, lifetime: {}", lifetime);
                return lifetime;
            }
        }
        return 0;
    }

    @NotNull
    private List<CustomScriptConfiguration> getScripts(@NotNull ExternalScriptContext context) {
        final Client client = context.getExecutionContext().getClient();
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty() || client == null) {
            log.trace("No LogoutStatusJwtType scripts or client is null.");
            return Lists.newArrayList();
        }

        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getLogoutStatusJwtScriptDns());
        if (!scripts.isEmpty()) {
            return scripts;
        }

        log.trace("No LogoutStatusJwtType scripts associated with client {}", client.getClientId());
        return Lists.newArrayList();
    }
}
