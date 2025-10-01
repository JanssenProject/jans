package io.jans.casa.conf;

import org.pf4j.PluginState;

import java.nio.file.Path;

/**
 * @author jgomer
 */
public class PluginInfo {

    private String id;
    private Path path;
    private PluginState state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public PluginState getState() {
        return state;
    }

    public void setState(PluginState state) {
        this.state = state;
    }

}
