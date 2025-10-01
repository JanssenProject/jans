package io.jans.model.custom.script.type.selectaccount;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummySelectAccountType implements SelectAccountType {

    @Override
    public String getSelectAccountPage(Object context) {
        return "";
    }

    @Override
    public boolean prepare(Object context) {
        return true;
    }

    @Override
    public String getAccountDisplayName(Object context) {
        return "";
    }

    @Override
    public boolean onSelect(Object context) {
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
        return 1;
    }
}
