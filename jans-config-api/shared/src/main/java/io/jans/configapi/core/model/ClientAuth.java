package io.jans.configapi.core.model;

import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.*;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAuth implements Serializable {

    private static final long serialVersionUID = -5224720733828296181L;
    
    private Map<Client, Set<Scope>> clientAuths;

    public Map<Client, Set<Scope>> getClientAuths() {
        return clientAuths;
    }

    public void setClientAuths(Map<Client, Set<Scope>> clientAuths) {
        this.clientAuths = clientAuths;
    }

    @Override
    public String toString() {
        return "ClientAuth [clientAuths=" + clientAuths + "]";
    }
}
