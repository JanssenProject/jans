package io.jans.agama.dsl;

import java.util.List;

public class TranspilationResult {

    /**
     * Name of the javascript function generated
     */
    private String funcName;
    
    /**
     * Parameter names the function has (as passed in the Input directive of the flow in question)
     */
    private List<String> inputs;
    
    /**
     * Number used in the Timeout directive (if present) 
     */
    private Integer timeout;
    
    /**
     * Actual function code. Remaining items, if any, correspond to the input
     */
    private String code;

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
}