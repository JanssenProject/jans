package io.jans.ca.rs.protect;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/04/2016
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {

    @JsonProperty(value = "httpMethods")
    List<String> httpMethods;
    @JsonProperty(value = "scopes")
    List<String> scopes;
    @JsonProperty(value = "scope_expression")
    JsonNode scope_expression;
    @JsonProperty(value = "ticketScopes")
    List<String> ticketScopes;

    public List<String> getTicketScopes() {
        return ticketScopes;
    }

    public void setTicketScopes(List<String> ticketScopes) {
        this.ticketScopes = ticketScopes;
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

    public JsonNode getScopeExpression() {
        return scope_expression;
    }

    public void setScopeExpression(JsonNode scopeExpression) {
        this.scope_expression = scopeExpression;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Condition");
        sb.append("{httpMethods=").append(httpMethods);
        sb.append(", scopes=").append(scopes);
        sb.append(", scope_expression=").append(scope_expression);
        sb.append(", ticketScopes=").append(ticketScopes);
        sb.append('}');
        return sb.toString();
    }
}
