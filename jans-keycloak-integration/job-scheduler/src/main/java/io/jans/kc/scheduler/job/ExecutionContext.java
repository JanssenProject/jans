package io.jans.kc.scheduler.job;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class ExecutionContext {
    
    private Map<String,Object> executionParams;

    public ExecutionContext() {

        this.executionParams = new HashMap<String,Object>();
    }

    protected Object getExecutionParameter(String key) {

        return executionParams.get(key);
    }

    protected void setExecutionParameter(String key, Object value) {

        executionParams.put(key,value);
    }
}
