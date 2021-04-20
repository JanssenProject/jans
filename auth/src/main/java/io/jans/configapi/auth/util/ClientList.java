package io.jans.configapi.auth.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

import io.jans.as.common.model.registration.Client;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientList implements Serializable {

    @JsonProperty(value = "clients")
    private List<Client> clients = Lists.newArrayList();

    public ClientList() {
    }

    public ClientList(List<Client> clients) {
        this.clients = clients;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClientList");
        sb.append("{clients=").append(clients);
        sb.append('}');
        return sb.toString();
    }
}
