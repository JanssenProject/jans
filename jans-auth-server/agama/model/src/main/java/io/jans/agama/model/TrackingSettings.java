package io.jans.agama.model;

import java.util.List;

public class TrackingSettings {

    private boolean enabled;
    private boolean skipTemplateViews;
    private boolean skipInstructionFrequencies;
    private List<String> excludedFlows;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSkipTemplateViews() {
        return skipTemplateViews;
    }

    public void setSkipTemplateViews(boolean skipTemplateViews) {
        this.skipTemplateViews = skipTemplateViews;
    }

    public boolean isSkipInstructionFrequencies() {
        return skipInstructionFrequencies;
    }

    public void setSkipInstructionFrequencies(boolean skipInstructionFrequencies) {
        this.skipInstructionFrequencies = skipInstructionFrequencies;
    }

    public List<String> getExcludedFlows() {
        return excludedFlows;
    }

    public void setExcludedFlows(List<String> excludedFlows) {
        this.excludedFlows = excludedFlows;
    }

}
