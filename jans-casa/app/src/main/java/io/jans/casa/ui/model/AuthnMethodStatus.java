package io.jans.casa.ui.model;

import org.zkoss.util.Pair;

import java.util.List;

/**
 * @author jgomer
 */
public class AuthnMethodStatus {

    private boolean enabled;
    private boolean deactivable;
    private String acr;
    private String name;
    private String selectedPlugin;
    private List<Pair<String, String>> plugins;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDeactivable() {
        return deactivable;
    }

    public String getAcr() {
        return acr;
    }

    public String getName() {
        return name;
    }

    public String getSelectedPlugin() {
        return selectedPlugin;
    }

    public List<Pair<String, String>> getPlugins() {
        return plugins;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDeactivable(boolean deactivable) {
        this.deactivable = deactivable;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSelectedPlugin(String selectedPlugin) {
        this.selectedPlugin = selectedPlugin;
    }

    public void setPlugins(List<Pair<String, String>> plugins) {
        this.plugins = plugins;
    }

}
