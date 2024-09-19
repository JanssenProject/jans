package io.jans.casa.ui.model;

import java.util.List;

import org.zkoss.util.Pair;

public class AuthnMethodStatus {

    private boolean enabled;
    private String acr;
    private String name;
    private String className; 
    private String selectedPlugin;
    private String description;
    private List<Pair<String, String>> plugins;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAcr() {
        return acr;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getSelectedPlugin() {
        return selectedPlugin;
    }

    public String getDescription() {
        return description;
    }

    public List<Pair<String, String>> getPlugins() {
        return plugins;
    }

    public void setEnabled(boolean enabled) {      
        this.enabled = enabled;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSelectedPlugin(String selectedPlugin) {
        this.selectedPlugin = selectedPlugin;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPlugins(List<Pair<String, String>> plugins) {
        this.plugins = plugins;
    }

}
