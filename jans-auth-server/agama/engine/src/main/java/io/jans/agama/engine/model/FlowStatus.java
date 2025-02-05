package io.jans.agama.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FlowStatus {
    
    public static final long PREPARED = 0;
    public static final long FINISHED = -1;
    
    private String qname;
    private String templatePath;
    private long startedAt;
    private long finishBefore;
    private boolean nativeClient;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object templateDataModel;

    private Deque<Map<String, String>> parentsMappings = new LinkedList<>();
    private String externalRedirectUrl;
    private boolean allowCallbackResume;
    private String jsonInput;
    private String startUrl;
    
    private FlowResult result;

    public Object getTemplateDataModel() {
        return templateDataModel;
    }

    public void setTemplateDataModel(Object templateDataModel) {
        this.templateDataModel = templateDataModel;
    }

    public FlowResult getResult() {
        return result;
    }

    public void setResult(FlowResult result) {
        this.result = result;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getFinishBefore() {
        return finishBefore;
    }

    public void setFinishBefore(long finishBefore) {
        this.finishBefore = finishBefore;
    }

    public boolean isNativeClient() {
        return nativeClient;
    }

    public void setNativeClient(boolean nativeClient) {
        this.nativeClient = nativeClient;
    }

    public String getQname() {
        return qname;
    }

    public void setQname(String qname) {
        this.qname = qname;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getExternalRedirectUrl() {
        return externalRedirectUrl;
    }

    public void setExternalRedirectUrl(String externalRedirectUrl) {
        this.externalRedirectUrl = externalRedirectUrl;
    }

    public boolean isAllowCallbackResume() {
        return allowCallbackResume;
    }

    public void setAllowCallbackResume(boolean allowCallbackResume) {
        this.allowCallbackResume = allowCallbackResume;
    }

    public Deque<Map<String, String>> getParentsMappings() {
        return parentsMappings;
    }

    public void setParentsMappings(Deque<Map<String, String>> parentsMappings) {
        this.parentsMappings = parentsMappings;
    }

    public String getJsonInput() {
        return jsonInput;
    }

    public void setJsonInput(String jsonInput) {
        this.jsonInput = jsonInput;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }

}
