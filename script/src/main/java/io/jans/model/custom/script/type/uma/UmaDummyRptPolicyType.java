package io.jans.model.custom.script.type.uma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.uma.ClaimDefinition;

/**
 * @author yuriyz on 05/30/2017.
 */
public class UmaDummyRptPolicyType implements UmaRptPolicyType {

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
    public List<ClaimDefinition> getRequiredClaims(Object authorizationContext) {
        return new ArrayList<ClaimDefinition>();
    }

    @Override
    public boolean authorize(Object authorizationContext) {
        return false;
    }

    @Override
    public String getClaimsGatheringScriptName(Object authorizationContext) {
        return "";
    }
}
