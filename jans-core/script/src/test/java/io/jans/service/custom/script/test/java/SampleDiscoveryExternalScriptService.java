package io.jans.service.custom.script.test.java;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.service.custom.script.ExternalScriptService;
import org.json.JSONObject;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SampleDiscoveryExternalScriptService extends ExternalScriptService {

    public SampleDiscoveryExternalScriptService() {
        super(CustomScriptType.DISCOVERY);
    }

    public boolean modifyDiscovery(JSONObject jsonObject, Object context) {
        final CustomScriptConfiguration script = getDefaultExternalCustomScript();
        if (script == null) {
            log.trace("No discovery java script set.");
            return false;
        }

        try {
            log.trace("Executing java 'modifyDiscovery' method, script name: {}, jsonWebResponse: {}, context: {}", script.getName(), jsonObject, context);

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
