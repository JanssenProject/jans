package org.gluu.oxd.server.model;

import com.google.common.collect.Lists;
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
    private List<String> httpMethods = Lists.newArrayList();
    @JsonProperty(value = "scopes")
    private List<String> scopes = Lists.newArrayList();
    @JsonProperty(value = "scope_expressions")
    private List<String> scopeExpressions = Lists.newArrayList();
    @JsonProperty(value = "ticketScopes")
    private List<String> ticketScopes = Lists.newArrayList();

    public List<String> getScopeExpressions() {
        if (scopeExpressions == null) {
            scopeExpressions = Lists.newArrayList();
        }
        return scopeExpressions;
    }

    public void setScopeExpressions(List<String> scopeExpressions) {
        this.scopeExpressions = scopeExpressions;
    }

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
        if (httpMethods == null) {
            httpMethods = Lists.newArrayList();
        }
        return httpMethods;
    }

    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public List<String> getScopes() {
        if (scopes == null) {
            scopes = Lists.newArrayList();
        }
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getTicketScopes() {
        if (ticketScopes == null) {
            ticketScopes = Lists.newArrayList();
        }
        return ticketScopes;
    }

    public void setTicketScopes(List<String> ticketScopes) {
        this.ticketScopes = ticketScopes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UmaResource");
        sb.append("{id='").append(id).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", httpMethods=").append(httpMethods);
        sb.append(", scopes=").append(scopes);
        sb.append(", scopeExpressions=").append(scopeExpressions);
        sb.append(", ticketScopes=").append(ticketScopes);
        sb.append('}');
        return sb.toString();
    }
}
