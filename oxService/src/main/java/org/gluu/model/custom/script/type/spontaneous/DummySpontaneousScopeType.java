package org.gluu.model.custom.script.type.spontaneous;

import org.gluu.model.SimpleCustomProperty;

import java.util.Map;

public class DummySpontaneousScopeType implements SpontaneousScopeType {

    @Override
    public boolean allowScope(Object context) {
        return false;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 0;
    }
}
