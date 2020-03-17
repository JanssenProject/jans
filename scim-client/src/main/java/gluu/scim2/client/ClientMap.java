package gluu.scim2.client;

import javax.ws.rs.client.Client;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A singleton object that holds a mapping of the RestEasy clients used by objects that extend the
 * {@link AbstractScimClient AbstractScimClient} class and the access tokens they are using.
 * <p>The method {@link AbstractScimClient#invoke(Object, Method, Object[]) AbstractScimClient#invoke(Object, Method, Object[])}
 * takes charge of the maintainance of this map at every service invocation, while the class
 * {@link gluu.scim2.client.rest.provider.AuthorizationInjectionFilter AuthorizationInjectionFilter} makes use of this
 * object to properly inject access tokens in authorization headers.</p>
 */
/*
 * Created by jgomer on 2017-11-25.
 */
public class ClientMap {

    private static ClientMap map = new ClientMap();

    private static Map<Client, String> mappings = new HashMap<>();

    private ClientMap() {
    }

    /**
     * Puts a new client/value pair in the map. If client already exists, the value is replaced.
     *
     * @param client RestEasy client
     * @param value  Value to associate to this client - normally an access token
     */
    public static void update(Client client, String value) {
        mappings.put(client, value);
    }

    /**
     * Removes a client from the map and then calls its close method to free resources.
     *
     * @param client Client to remove
     */
    public static void remove(Client client) {
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
    public static String getValue(Client client) {
        return mappings.get(client);
    }

    /**
     * Flushes the client/value map (it will be empty after invocation).
     * Call this method when your application is being shut-down.
     */
    public static void clean() {
        for (Client client : mappings.keySet()) {
            remove(client);
        }
    }

}
