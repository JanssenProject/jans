package io.jans.model.custom.script.type.token;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummyTokenExchangeType implements TokenExchangeType {
    @Override
    public boolean modifyResponse(Object context) {
        return false;
    }

    @Override
    public void validate(Object context) {
        // dummy
    }

    @Override
    public boolean skipBuiltinValidation(Object context) {
        return false;
    }

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
}
