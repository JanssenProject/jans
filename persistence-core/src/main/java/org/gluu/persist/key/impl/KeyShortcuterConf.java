package org.gluu.persist.key.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class KeyShortcuterConf {

    @JsonProperty
    private List<String> prefixes;
    @JsonProperty
    private Map<String, String> replaces;
    @JsonProperty
    private List<String> exclusions;

    public List<String> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    public Map<String, String> getReplaces() {
        return replaces;
    }

    public void setReplaces(Map<String, String> replaces) {
        this.replaces = replaces;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

    @Override
    public String toString() {
        return "KeyShortcuterConf{" +
                "prefixes=" + prefixes +
                ", replaces=" + replaces +
                ", exclusions=" + exclusions +
                '}';
    }
}
