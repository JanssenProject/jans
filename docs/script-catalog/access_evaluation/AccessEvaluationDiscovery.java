/* Copyright (c) 2024, Gluu
 Author: Yuriy Z
 */

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzen.AccessEvaluationDiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AccessEvaluationDiscovery implements AccessEvaluationDiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(AccessEvaluationDiscovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public int getApiVersion() {
        log.info("getApiVersion AccessEvaluationDiscovery Java custom script: 11");
        return 11;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("write to script logger");
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_java", "value_from_script_on_java");
        return true;
    }
}

