package org.gluu.oxauth.service.external;

import com.google.common.collect.Lists;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.introspection.IntrospectionType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.external.context.ExternalIntrospectionContext;
import org.gluu.service.custom.script.ExternalScriptService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
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
public class ExternalIntrospectionService extends ExternalScriptService {

    private static final long serialVersionUID = -8609727759114795446L;

    @Inject
    private Logger log;
    @Inject
    private AppConfiguration appConfiguration;

    public ExternalIntrospectionService() {
        super(CustomScriptType.INTROSPECTION);
    }

    @NotNull
    private List<CustomScriptConfiguration> getScripts(@Nullable Client client) {
        if (customScriptConfigurations == null) {
            return Lists.newArrayList();
        }
        if (appConfiguration.getIntrospectionScriptBackwardCompatibility() || client == null) {
            return customScriptConfigurations;
        }

        return getCustomScriptConfigurationsByDns(client.getAttributes().getIntrospectionScripts());
    }

    public boolean executeExternalModifyResponse(JSONObject responseAsJsonObject, ExternalIntrospectionContext context) {
        final List<CustomScriptConfiguration> scripts = getScripts(context.getTokenGrant().getClient());
        if (scripts.isEmpty()) {
            log.debug("There is no any external interception scripts defined.");
            return false;
        }

        for (CustomScriptConfiguration script : scripts) {
            if (!executeExternalModifyResponse(script, responseAsJsonObject, context)) {
                log.debug("Stopped running external interception scripts because script {} returns false.", script.getName());
                return false;
            }
        }

        return true;
    }

    private boolean executeExternalModifyResponse(CustomScriptConfiguration scriptConf, JSONObject responseAsJsonObject, ExternalIntrospectionContext context) {
        try {
            log.debug("Executing external 'executeExternalModifyResponse' method, script name: {}, responseAsJsonObject: {} , context: {}",
                    scriptConf.getName(), responseAsJsonObject, context);

            IntrospectionType script = (IntrospectionType) scriptConf.getExternalType();
            context.setScript(scriptConf);
            final boolean result = script.modifyResponse(responseAsJsonObject, context);
            log.debug("Finished external 'executeExternalModifyResponse' method, script name: {}, responseAsJsonObject: {} , context: {}, result: {}",
                    scriptConf.getName(), responseAsJsonObject, context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConf.getCustomScript(), ex);
            return false;
        }
    }
}
