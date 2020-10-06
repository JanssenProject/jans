package io.jans.model.custom.script.type.owner;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DummyResourceOwnerPasswordCredentialsType implements ResourceOwnerPasswordCredentialsType {
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
    public boolean authenticate(Object context) {
        return false;
    }
}
