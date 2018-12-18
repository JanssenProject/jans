package org.xdi.model.custom.script.type.introspection;

import org.xdi.model.SimpleCustomProperty;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DummyIntrospectionType implements IntrospectionType {
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
        return 1;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object introspectionContext) {
        return false;
    }
}
