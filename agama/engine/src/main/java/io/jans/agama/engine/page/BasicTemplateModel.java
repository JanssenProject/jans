package io.jans.agama.engine.page;

public class BasicTemplateModel {

    private String message;
    private String flowName;

    public BasicTemplateModel(String message) {
        this.message = message;
    }

    public BasicTemplateModel(String message, String flowName) {
        this.message = message;
        this.flowName = flowName;
    }

    public String getMessage() {
        return message;
    }

    public String getFlowName() {
        return flowName;
    }
    
}
