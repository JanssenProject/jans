package io.jans.configapi.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomScope extends Scope {
    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    private List<Client> clients;

    @Override
    public String toString() {
        return "CustomScope [clients=" + clients + "]";
    }
    
    

}
