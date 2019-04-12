package org.gluu.oxauth.service.external;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import org.gluu.oxauth.service.external.context.ExternalResourceOwnerPasswordCredentialsContext;
import org.gluu.service.custom.script.ExternalScriptService;
import org.slf4j.Logger;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalResourceOwnerPasswordCredentialsService extends ExternalScriptService {

    private static final long serialVersionUID = -1070021905117551202L;

    @Inject
    private Logger log;

    public ExternalResourceOwnerPasswordCredentialsService() {
        super(CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
    }

    public boolean executeExternalAuthenticate(ExternalResourceOwnerPasswordCredentialsContext context) {
        if (customScriptConfigurations == null || customScriptConfigurations.isEmpty()) {
            log.debug("There is no any external interception scripts defined.");
            return false;
        }

        for (CustomScriptConfiguration script : customScriptConfigurations) {
            if (!executeExternalAuthenticate(script, context)) {
                log.debug("Stopped running external RO PC scripts because script {} returns false.", script.getName());
                return false;
            }
        }

        return true;
    }

    private boolean executeExternalAuthenticate(CustomScriptConfiguration customScriptConfiguration, ExternalResourceOwnerPasswordCredentialsContext context) {
        try {
            log.debug("Executing external 'executeExternalAuthenticate' method, script name: {}, context: {}",
                    customScriptConfiguration.getName(), context);

            ResourceOwnerPasswordCredentialsType script = (ResourceOwnerPasswordCredentialsType) customScriptConfiguration.getExternalType();
            context.setScript(customScriptConfiguration);
            final boolean result = script.authenticate(context);

            log.debug("Finished external 'executeExternalAuthenticate' method, script name: {}, context: {}, result: {}",
                    customScriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

}
