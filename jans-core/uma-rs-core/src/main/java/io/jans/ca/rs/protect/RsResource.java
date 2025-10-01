package io.jans.ca.rs.protect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/12/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RsResource implements Serializable {

    @JsonProperty(value = "path")
    private String path;
    @JsonProperty(value = "conditions")
    private List<Condition> conditions;
    @JsonProperty(value = "iat")
    private Integer iat;
    @JsonProperty(value = "exp")
    private Integer exp;

    private Map<String, Condition> httpMethodToCondition = null;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<String> scopes(String httpMethod) {
        return getConditionMap().get(httpMethod).getScopes();
    }

    public Integer getIat() {
        return iat;
    }

    public void setIat(Integer iat) {
        this.iat = iat;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public JsonNode getScopeExpression(String httpMethod) {
        return getConditionMap().get(httpMethod).getScopeExpression();
    }

    public List<String> getScopesForTicket(String httpMethod) {
        Condition condition = getConditionMap().get(httpMethod);
        if (condition.getScopeExpression() != null) {
            final JsonLogicNode node = parseNode(condition.getScopeExpression().toString());
            if (node != null) {
                return node.getData(); // return all scopes defined in "data" of json object
            }
        }
        return condition.getTicketScopes() != null && !condition.getTicketScopes().isEmpty() ?
                condition.getTicketScopes() : condition.getScopes();
    }

    public static JsonLogicNode parseNode(String json) {
        try {
            return Jackson.createJsonMapper().readValue(json, JsonLogicNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Condition> getConditionMap() {
        if (httpMethodToCondition == null) {
            initMap();
        }
        return httpMethodToCondition;
    }

    private void initMap() {
        httpMethodToCondition = Maps.newHashMap();
        if (conditions != null) {
            for (Condition condition : conditions) {
                if (condition.getHttpMethods() != null) {
                    for (String httpMethod : condition.getHttpMethods()) {
                        httpMethodToCondition.put(httpMethod, condition);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsResource");
        sb.append("{path='").append(path).append('\'');
        sb.append(", conditions=").append(conditions);
        sb.append(", httpMethodToCondition=").append(httpMethodToCondition);
        sb.append(", iat=").append(iat);
        sb.append(", exp=").append(exp);
        sb.append('}');
        return sb.toString();
    }
}
