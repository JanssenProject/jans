package io.jans.as.server.service.external;

import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.revoke.RevokeTokenType;
import io.jans.service.custom.script.ExternalScriptService;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalRevokeTokenService extends ExternalScriptService {

    public ExternalRevokeTokenService() {
        super(CustomScriptType.REVOKE_TOKEN);
    }

    public boolean revokeToken(CustomScriptConfiguration script, ExecutionContext context) {
        try {
            log.trace("Executing python 'revokeToken' method, context: {}", context);
            context.setScript(script);
            RevokeTokenType revokeTokenType = (RevokeTokenType) script.getExternalType();
            final boolean result = revokeTokenType.revoke(context);
            log.trace("Finished 'revokeToken' method, result: {}, context: {}", result, context);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        return false;
    }

    public boolean revokeTokenMethods(ExecutionContext context) {
        for (CustomScriptConfiguration script : this.customScriptConfigurations) {
            if (script.getExternalType().getApiVersion() > 1 && !revokeToken(script, context)) {
                return false;
            }
        }
        return true;
    }
}
