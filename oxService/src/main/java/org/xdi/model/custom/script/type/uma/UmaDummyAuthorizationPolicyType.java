package org.xdi.model.custom.script.type.uma;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.ScriptContext;
import org.xdi.model.uma.ClaimDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 05/30/2017.
 */
public class UmaDummyAuthorizationPolicyType implements UmaAuthorizationPolicyType {
    @Override
    public List<ClaimDefinition> getRequiredClaims() {
        return new ArrayList<ClaimDefinition>();
    }

    @Override
    public boolean authorize(Object authorizationContext, Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public int getNextStep(ScriptContext scriptContext) {
        return -1;
    }

    @Override
    public boolean prepareForStep(ScriptContext scriptContext) {
        return false;
    }

    @Override
    public int getStepsCount(ScriptContext scriptContext) {
        return -1;
    }

    @Override
    public String getPageForStep(ScriptContext scriptContext) {
        return null;
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
        return 1;
    }
}
