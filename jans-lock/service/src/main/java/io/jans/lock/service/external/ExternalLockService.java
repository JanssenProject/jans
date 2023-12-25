package io.jans.lock.service.external;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.external.context.ExternalLockContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.lock.LockExtensionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * External lock service
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@ApplicationScoped
@DependsOn("appInitializer")
public class ExternalLockService extends ExternalScriptService {

    public ExternalLockService() {
        super(CustomScriptType.LOCK_EXTENSION);
    }

    public boolean beforeDataPut(JsonNode params, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforeDataPut(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (beforeDataPut) from external lock script");
        return true;
    }


    public boolean beforePolicyPut(JsonNode params, ExternalLockContext context) {
        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!beforePolicyPut(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (beforePolicyPut) from external lock script");
        return true;
    }

    private boolean beforeDataPut(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforeDataPut' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforeDataPut(params, context);
            return context.isCancelOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean beforePolicyPut(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalLockContext context) {
        try {
            log.trace("Executing external 'beforePolicyPut' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            LockExtensionType script = (LockExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            script.beforePolicyPut(params, context);
            return context.isCancelOperation();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

}
