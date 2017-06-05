package org.xdi.model.custom.script.type.uma;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.ScriptContext;
import org.xdi.model.custom.script.type.BaseExternalType;
import org.xdi.model.uma.ClaimDefinition;

import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 05/30/2017.
 */
public interface UmaAuthorizationPolicyType extends BaseExternalType {

    List<ClaimDefinition> getRequiredClaims();

    boolean authorize(Object authorizationContext, Map<String, SimpleCustomProperty> configurationAttributes);

    int getNextStep(ScriptContext scriptContext);

    boolean prepareForStep(ScriptContext scriptContext);

    int getStepsCount(ScriptContext scriptContext);

    String getPageForStep(ScriptContext scriptContext);

}
