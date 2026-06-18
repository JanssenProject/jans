package io.jans.as.server.service.external;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.token.IdentityAssertionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.json.JSONObject;

import java.util.List;

/**
 * External script service for Identity Assertion JWT Authorization Grant (ID-JAG).
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalIdentityAssertionService extends ExternalScriptService {

    public ExternalIdentityAssertionService() {
        super(CustomScriptType.IDENTITY_ASSERTION);
    }

    public boolean externalModifyIdJagPayload(Jwt idJag, ExecutionContext context) {
        final Client client = context.getClient();
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getIdJagScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} identity-assertion scripts for modifyIdJagPayload.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalModifyIdJagPayload(idJag, script, context)) {
                return false;
            }
        }
        return true;
    }

    public boolean externalModifyIdJagPayload(Jwt idJag, CustomScriptConfiguration script, ExecutionContext context) {
        final Client client = context.getClient();
        log.trace("Executing external 'modifyIdJagPayload', script name: {}, clientId: {}", script.getName(), client.getClientId());

        boolean result = false;
        try {
            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            IdentityAssertionType identityAssertionType = (IdentityAssertionType) script.getExternalType();
            result = identityAssertionType.modifyIdJagPayload(idJag, scriptContext);

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

        log.trace("Finished 'modifyIdJagPayload', script name: {}, clientId: {}, result: {}", script.getName(), client.getClientId(), result);
        return result;
    }

    public boolean externalModifyResponse(JSONObject response, ExecutionContext context) {
        final Client client = context.getClient();
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getIdJagScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} identity-assertion scripts for modifyResponse.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalModifyResponse(response, script, context)) {
                return false;
            }
        }
        return true;
    }

    public boolean externalModifyResponse(JSONObject response, CustomScriptConfiguration script, ExecutionContext context) {
        final Client client = context.getClient();
        log.debug("Executing external 'modifyResponse', script name: {}, clientId: {}, responseKeys: {}", script.getName(), client.getClientId(), response.keySet());

        boolean result = false;
        try {
            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            IdentityAssertionType identityAssertionType = (IdentityAssertionType) script.getExternalType();
            result = identityAssertionType.modifyResponse(response, scriptContext);

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

        log.debug("Finished 'modifyResponse', script name: {}, clientId: {}, result: {}, responseKeys: {}", script.getName(), client.getClientId(), result, response.keySet());
        return result;
    }
}
