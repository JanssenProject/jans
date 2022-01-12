package io.jans.model.custom.script.type.revoke;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DummyRevokeTokenType implements RevokeTokenType {
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
    public boolean revoke(Object context) {
        return true;
    }
}
