package io.jans.model.custom.script.type.authzdetails;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummyAuthzDetail implements AuthzDetailType {
    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public int getApiVersion() {
        return 0;
    }

    @Override
    public boolean validateDetail(Object context) {
        return true;
    }

    @Override
    public String getUiRepresentation(Object context) {
        return "";
    }
}
