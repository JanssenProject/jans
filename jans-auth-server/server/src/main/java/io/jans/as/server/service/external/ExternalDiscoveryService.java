package io.jans.as.server.service.external;

import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.service.custom.script.ExternalScriptService;
import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ExternalDiscoveryService extends ExternalScriptService {

    public ExternalDiscoveryService() {
        super(CustomScriptType.DISCOVERY);
    }

    public boolean modifyDiscovery(JSONObject jsonObject, ExecutionContext context) {
        final CustomScriptConfiguration script = getDefaultExternalCustomScript();
        if (script == null) {
            log.trace("No discovery script set.");
            return false;
        }

        try {
            log.trace("Executing python 'modifyDiscovery' method, script name: {}, jsonWebResponse: {}, context: {}", script.getName(), jsonObject, context);
            context.setScript(script);

            DiscoveryType discoveryType = (DiscoveryType) script.getExternalType();
            final boolean result = discoveryType.modifyResponse(jsonObject, context);
            log.trace("Finished 'modifyDiscovery' method, script name: {}, jsonWebResponse: {}, context: {}, result: {}", script.getName(), jsonObject, context, result);

            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return false;
    }
}
