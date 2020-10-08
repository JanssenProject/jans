package org.gluu.oxauth.service.external;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.service.custom.script.ExternalScriptService;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.external.context.ExternalPostAuthnContext;
import org.slf4j.Logger;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalPostAuthnService  extends ExternalScriptService {

    @Inject
    private Logger log;

    public ExternalPostAuthnService() {
        super(CustomScriptType.POST_AUTHN);
    }

    public boolean externalForceReAuthentication(Client client, ExternalPostAuthnContext context) {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} post-authn scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalForceReAuthentication(script, context)) {
                return false;
            }
        }

        log.debug("Forcing re-authentication via post-authn script.");
        return true;
    }

    public boolean externalForceAuthorization(Client client, ExternalPostAuthnContext context) {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurationsByDns(client.getAttributes().getPostAuthnScripts());
        if (scripts.isEmpty()) {
            return false;
        }
        log.trace("Found {} post-authn scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            if (!externalForceAuthorization(script, context)) {
                return false;
            }
        }

        log.debug("Forcing authorization via post-authn script.");
        return true;
    }


    public boolean externalForceReAuthentication(CustomScriptConfiguration scriptConfiguration, ExternalPostAuthnContext context) {
        try {
            log.trace("Executing external 'externalForceReAuthentication' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            PostAuthnType script = (PostAuthnType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.forceReAuthentication(context);

            log.trace("Finished external 'externalForceReAuthentication' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public boolean externalForceAuthorization(CustomScriptConfiguration scriptConfiguration, ExternalPostAuthnContext context) {
        try {
            log.trace("Executing external 'externalForceAuthorization' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            PostAuthnType script = (PostAuthnType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.forceAuthorization(context);

            log.trace("Finished external 'externalForceAuthorization' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }
}
