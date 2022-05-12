package io.jans.agama.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ParentFlowData {

    private String parentBasepath;
    private String[] pathOverrides;

    public String getParentBasepath() {
        return parentBasepath;
    }

    public void setParentBasepath(String parentBasepath) {
        this.parentBasepath = parentBasepath;
    }

    public String[] getPathOverrides() {
        return pathOverrides;
    }

    public void setPathOverrides(String[] pathOverrides) {
        this.pathOverrides = pathOverrides;
    }
    
}
