package io.jans.fido2.service.external;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.service.external.context.ExternalFido2Context;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.fido2.Fido2ExtensionType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
public class ExternalFido2Service extends ExternalScriptService {

    public ExternalFido2Service() {
        super(CustomScriptType.FIDO2_EXTENSION);
    }

    public boolean registerAttestationStart(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("registerAttestation"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!registerAttestationStart(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (registerAttestation) from external fido2-response script.");
        return true;
    }

    public boolean registerAttestationFinish(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("registerAttestationFinish"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!registerAttestationFinish(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (registerAttestationFinish) from external fido2-response script.");
        return true;
    }

    public boolean verifyAttestationStart(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("verifyAttestation"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!verifyAttestationStart(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (verifyAttestation) from external fido2-response script.");
        return true;
    }

    public boolean verifyAttestationFinish(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("verifyAttestationFinish"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!verifyAttestationFinish(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (verifyAttestationFinish) from external fido2-response script.");
        return true;
    }

    public boolean authenticateAssertionStart(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("authenticateAssertion"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!authenticateAssertionStart(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (authenticateAssertion) from external fido2-response script.");
        return true;
    }

    public boolean authenticateAssertionFinish(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("authenticateAssertionFinish"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!authenticateAssertionFinish(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (authenticateAssertionFinish) from external fido2-response script.");
        return true;
    }

    public boolean verifyAssertionStart(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("verifyAssertion"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!verifyAssertionStart(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (verifyAssertion) from external fido2-response script.");
        return true;
    }

    public boolean verifyAssertionFinish(JsonNode params, ExternalFido2Context context) {
        if (!verifyCustomScripts("verifyAssertionFinish"))
            return true;

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!verifyAssertionFinish(params, script, context)) {
                return false;
            }
        }

        log.debug("Executed (verifyAssertionFinish) from external fido2-response script.");
        return true;
    }

    private boolean registerAttestationStart(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'registerAttestation' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.registerAttestationStart(params, context);

            log.trace("Finished external 'registerAttestation' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean registerAttestationFinish(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'registerAttestationFinish' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.registerAttestationFinish(params, context);

            log.trace("Finished external 'registerAttestationFinish' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyAttestationStart(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'verifyAttestation' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.verifyAttestationStart(params, context);
            log.trace("Finished external 'verifyAttestation' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyAttestationFinish(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'verifyAttestationFinish' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.verifyAttestationFinish(params, context);
            log.trace("Finished external 'verifyAttestationFinish' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyAssertionStart(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'verifyAssertion' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.verifyAssertionStart(params, context);

            log.trace("Finished external 'verifyAssertion' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyAssertionFinish(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'verifyAssertionFinish' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.verifyAssertionFinish(params, context);

            log.trace("Finished external 'verifyAssertionFinish' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean authenticateAssertionStart(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'authenticateAssertion' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.authenticateAssertionStart(params, context);

            log.trace("Finished external 'authenticateAssertion' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);

            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean authenticateAssertionFinish(JsonNode params, CustomScriptConfiguration scriptConfiguration, ExternalFido2Context context) {
        try {
            log.trace("Executing external 'authenticateAssertionFinish' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            Fido2ExtensionType script = (Fido2ExtensionType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.authenticateAssertionFinish(params, context);

            log.trace("Finished external 'authenticateAssertionFinish' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);

            context.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    private boolean verifyCustomScripts(String titleContext) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.trace("There is no any external fido2-response scripts defined (" + titleContext + ").");
            return false;
        }
        log.trace("Found {} external fido2-response scripts defined (" + titleContext + ").", customScriptConfigurations.size());
        return true;
    }

}
