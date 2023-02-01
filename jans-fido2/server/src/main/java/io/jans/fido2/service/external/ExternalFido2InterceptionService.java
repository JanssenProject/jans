package io.jans.fido2.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.service.external.context.ExternalFido2InterceptionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.fido2.Fido2InterceptionType;
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
public class ExternalFido2InterceptionService extends ExternalScriptService {

    public ExternalFido2InterceptionService() {
        super(CustomScriptType.FIDO2_INTERCEPTION);
    }

    public boolean interceptRegisterAttestation(JsonNode params, ExternalFido2InterceptionContext context) {
        if (!verifyCustomScripts("interceptRegisterAttestation"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!interceptRegisterAttestation(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (interceptRegisterAttestation) from external fido2-interception script.");
        return true;
    }

    public boolean interceptVerifyAttestation(JsonNode params, ExternalFido2InterceptionContext context) {
        if (!verifyCustomScripts("interceptVerifyAttestation"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!interceptVerifyAttestation(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (interceptVerifyAttestation) from external fido2-interception script.");
        return true;
    }

    public boolean interceptAuthenticateAssertion(JsonNode params, ExternalFido2InterceptionContext context) {
        if (!verifyCustomScripts("interceptAuthenticateAssertion"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!interceptAuthenticateAssertion(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (interceptAuthenticateAssertion) from external fido2-interception script.");
        return true;
    }

    public boolean interceptVerifyAssertion(JsonNode params, ExternalFido2InterceptionContext context) {
        if (!verifyCustomScripts("interceptVerifyAssertion"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!interceptVerifyAssertion(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (interceptVerifyAssertion) from external fido2-interception script.");
        return true;
    }


    private boolean interceptRegisterAttestation(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2InterceptionContext context) {
        try {
            log.trace("Executing external 'interceptRegisterAttestation' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2InterceptionType script = (Fido2InterceptionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.interceptRegisterAttestation(params, context);

            log.trace("Finished external 'interceptRegisterAttestation' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean interceptVerifyAttestation(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2InterceptionContext context) {
        try {
            log.trace("Executing external 'interceptVerifyAttestation' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2InterceptionType script = (Fido2InterceptionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.interceptVerifyAttestation(params, context);

            log.trace("Finished external 'interceptVerifyAttestation' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean interceptVerifyAssertion(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2InterceptionContext context) {
        try {
            log.trace("Executing external 'interceptVerifyAssertion' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2InterceptionType script = (Fido2InterceptionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.interceptVerifyAssertion(params, context);

            log.trace("Finished external 'interceptVerifyAssertion' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }
    private boolean interceptAuthenticateAssertion(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2InterceptionContext context) {
        try {
            log.trace("Executing external 'interceptAuthenticateAssertion' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2InterceptionType script = (Fido2InterceptionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.interceptAuthenticateAssertion(params, context);

            log.trace("Finished external 'interceptAuthenticateAssertion' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyCustomScripts(String titleContext) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.trace("There is no any external fido2-interception scripts defined (" + titleContext + ").");
            return false;
        }
        log.trace("Found {} external fido2-interception scripts defined (" + titleContext + ").", customScriptConfigurations.size());
        return true;
    }
}
