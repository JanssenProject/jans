package io.jans.model.custom.script.type.createuser;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummyCreateUserType implements CreateUserType {

    @Override
    public String getCreateUserPage(Object context) {
        return "";
    }

    @Override
    public boolean prepare(Object context) {
        return false;
    }

    @Override
    public boolean createUser(Object context) {
        return false;
    }

    @Override
    public String buildPostAuthorizeUrl(Object context) {
        return null;
    }

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
        return 1;
    }
}
