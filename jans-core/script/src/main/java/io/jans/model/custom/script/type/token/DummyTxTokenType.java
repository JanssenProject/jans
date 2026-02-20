package io.jans.model.custom.script.type.token;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

public class DummyTxTokenType implements TxTokenType {


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
    public int getTxTokenLifetimeInSeconds(Object context) {
        return 0;
    }

    @Override
    public boolean modifyTokenPayload(Object jsonWebResponse, Object context) {
        return true;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        return true;
    }
}
