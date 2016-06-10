package org.xdi.oxd.server.model;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/06/2016
 */

public class UmaResource {

    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "path")
      private String path;
    @JsonProperty(value = "http_methods")
      private List<String> httpMethods;
    @JsonProperty(value = "scopes")
      private List<String> scopes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaResource");
        sb.append("{id='").append(id).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", httpMethods=").append(httpMethods);
        sb.append(", scopes=").append(scopes);
        sb.append('}');
        return sb.toString();
    }
}
