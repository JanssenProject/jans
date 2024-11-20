package io.jans.model.custom.script.type.authzen;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummyAccessEvaluationType implements AccessEvaluationType {

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

    @Override
    public AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object context) {
        return AccessEvaluationResponse.FALSE;
    }
}
