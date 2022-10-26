/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * A singleton object that holds a mapping of the RestEasy clients used by objects that extend the
 * {@link AbstractScimClient AbstractScimClient} class and the access tokens they are using.
 * <p>The method {@link AbstractScimClient#invoke(Object, Method, Object[]) AbstractScimClient#invoke(Object, Method, Object[])}
 * takes charge of the maintenance of this map at every service invocation, while the class
 * {@link io.jans.scim2.client.rest.provider.AuthorizationInjectionFilter AuthorizationInjectionFilter} makes use of this
 * object to properly inject access tokens in authorization headers.</p>
 */
public class ClientMap {

    private static ClientMap map = new ClientMap();

    private Map<Client, String> mappings = new HashMap<>();

    private Map<Client, MultivaluedMap<String, String>> customHeadersMap = new HashMap<>();

    private ClientMap() {
    }

    public static ClientMap instance() {
        return map;
    }

    /**
     * Puts a new client/value pair in the map. If client already exists, the value is replaced.
     *
     * @param client RestEasy client
     * @param value  Value to associate to this client - normally an access token
     */
    public void update(Client client, String value) {
        mappings.put(client, value);
    }

    /**
     * Removes a client from the map and then calls its close method to free resources.
     *
     * @param client Client to remove
     */
    public void remove(Client client) {
        //Frees the resources associated to this RestEasy client
        client.close();
        mappings.remove(client);
    }

    /**
     * Gets the value associated to this client in the map.
     *
     * @param client RestEasy client
     * @return A string value. If there is no entry for client in the map, returns null
     */
    public String getValue(Client client) {
        return mappings.get(client);
    }

    /**
     * Flushes the client/value map (it will be empty after invocation).
     * Call this method when your application is being shut-down.
     */
    public void clean() {
        customHeadersMap.clear();
        mappings.keySet().forEach(this::remove);
    }

    public MultivaluedMap<String, String> getCustomHeaders(Client client) {
        return customHeadersMap.get(client);
    }

    public void setCustomHeaders(Client client, MultivaluedMap<String, String> headersMap) {
        customHeadersMap.put(client, headersMap);
    }

}
