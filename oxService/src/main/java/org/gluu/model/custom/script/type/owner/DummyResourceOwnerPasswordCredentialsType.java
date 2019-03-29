package org.gluu.model.custom.script.type.owner;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DummyResourceOwnerPasswordCredentialsType implements ResourceOwnerPasswordCredentialsType {
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
    public boolean authenticate(Object context) {
        return false;
    }
}
