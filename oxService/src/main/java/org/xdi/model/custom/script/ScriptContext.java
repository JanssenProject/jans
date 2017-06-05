package org.xdi.model.custom.script;

import org.xdi.model.SimpleCustomProperty;

import java.util.Map;

/**
 * @author yuriyz on 05/30/2017.
 */
public class ScriptContext {

    private Map<String, SimpleCustomProperty> configurationAttributes;
    private Map<String, String[]> requestParameters;
    private int step;

    public ScriptContext() {
    }

    public ScriptContext(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
        this.configurationAttributes = configurationAttributes;
        this.requestParameters = requestParameters;
        this.step = step;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public void setConfigurationAttributes(Map<String, SimpleCustomProperty> configurationAttributes) {
        this.configurationAttributes = configurationAttributes;
    }

    public Map<String, String[]> getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(Map<String, String[]> requestParameters) {
        this.requestParameters = requestParameters;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
