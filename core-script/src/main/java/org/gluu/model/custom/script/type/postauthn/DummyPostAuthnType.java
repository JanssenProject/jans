package org.gluu.model.custom.script.type.postauthn;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.CustomScript;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DummyPostAuthnType implements PostAuthnType {
    @Override
    public boolean forceReAuthentication(Object context) {
        return false;
    }

    @Override
    public boolean forceAuthorization(Object context) {
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
        return 0;
    }
}
