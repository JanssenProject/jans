package io.jans.model.custom.script.type.authzchallenge;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummyAuthorizationChallengeType implements AuthorizationChallengeType {

    @Override
    public boolean authorize(Object context) {
        return false;
    }

    @Override
    public Map<String, String> getAuthenticationMethodClaims(Object context) {
        return new HashMap<>();
    }

    @Override
    public void prepareAuthzRequest(Object context) {
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
