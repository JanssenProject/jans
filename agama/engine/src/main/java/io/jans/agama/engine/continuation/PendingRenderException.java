package io.jans.agama.engine.continuation;

import java.util.Map;

import org.mozilla.javascript.NativeContinuation;

public class PendingRenderException extends PendingException {
    
    private String templatePath;
    private Map<String, Object> dataModel;

    public PendingRenderException(NativeContinuation continuation) {
        super(continuation);
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public Map<String, Object> getDataModel() {
        return dataModel;
    }

    public void setDataModel(Map<String, Object> dataModel) {
        this.dataModel = dataModel;
    }
    
}
