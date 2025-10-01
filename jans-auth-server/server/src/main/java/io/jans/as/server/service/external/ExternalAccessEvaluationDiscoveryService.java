package io.jans.as.server.service.external;

import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.authzen.AccessEvaluationDiscoveryType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import org.json.JSONObject;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ExternalAccessEvaluationDiscoveryService extends ExternalScriptService {

    public ExternalAccessEvaluationDiscoveryService() {
        super(CustomScriptType.ACCESS_EVALUATION_DISCOVERY);
    }

    public boolean modifyDiscovery(JSONObject jsonObject, ExecutionContext context) {
        final CustomScriptConfiguration script = getDefaultExternalCustomScript();
        if (script == null) {
            log.debug("No access evaluation discovery script set.");
            return false;
        }

        try {
            log.debug("Executing python 'modifyDiscovery' method, script name: {}, jsonObj: {}, context: {}", script.getName(), jsonObject, context);
            context.setScript(script);

            AccessEvaluationDiscoveryType discoveryType = (AccessEvaluationDiscoveryType) script.getExternalType();
            final boolean result = discoveryType.modifyResponse(jsonObject, context);
            log.debug("Finished 'modifyDiscovery' method, script name: {}, jsonObj: {}, context: {}, result: {}", script.getName(), jsonObject, context, result);

            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return false;
    }
}
