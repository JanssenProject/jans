package io.jans.agama.engine.continuation;

import org.mozilla.javascript.NativeContinuation;

public class PendingRenderException extends PendingException {
    
    private String templatePath;
    private Object dataModel;

    public PendingRenderException(NativeContinuation continuation) {
        super(continuation);
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public Object getDataModel() {
        return dataModel;
    }

    public void setDataModel(Object dataModel) {
        this.dataModel = dataModel;
    }
    
}
