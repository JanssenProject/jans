package io.jans.as.server.service.external;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.jans.as.server.service.external.context.RevokeTokenContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.revoke.RevokeTokenType;
import io.jans.service.custom.script.ExternalScriptService;

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

    public boolean revokeToken(CustomScriptConfiguration script, RevokeTokenContext context) {
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

    public boolean revokeTokenMethods(RevokeTokenContext context) {
        for (CustomScriptConfiguration script : this.customScriptConfigurations) {
            if (script.getExternalType().getApiVersion() > 1) {
                if (!revokeToken(script, context)) {
                    return false;
                }
            }
        }
        return true;
    }
}
