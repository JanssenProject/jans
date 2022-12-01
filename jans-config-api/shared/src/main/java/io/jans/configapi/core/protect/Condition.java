package io.jans.configapi.core.protect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {

    @JsonProperty(value = "httpMethods")
    List<String> httpMethods;
    @JsonProperty(value = "scopes")
    List<Scope> scopes;
    @JsonProperty(value = "groupScopes")
    List<Scope> groupScopes;
    @JsonProperty(value = "superScopes")
    List<Scope> superScopes;
    
    public List<String> getHttpMethods() {
        return httpMethods;
    }
    
    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }
    
    public List<Scope> getScopes() {
        return scopes;
    }
    
    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }
    
    public List<Scope> getGroupScopes() {
        return groupScopes;
    }
    
    public void setGroupScopes(List<Scope> groupScopes) {
        this.groupScopes = groupScopes;
    }
    
    public List<Scope> getSuperScopes() {
        return superScopes;
    }
    
    public void setSuperScopes(List<Scope> superScopes) {
        this.superScopes = superScopes;
    }
    
    @Override
    public String toString() {
        return "Condition [httpMethods=" + httpMethods + ", scopes=" + scopes + ", groupScopes=" + groupScopes
                + ", superScopes=" + superScopes + "]";
    }

   }
