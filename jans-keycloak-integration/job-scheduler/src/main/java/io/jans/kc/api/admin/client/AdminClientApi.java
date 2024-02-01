package io.jans.kc.api.admin.client;

import io.jans.kc.api.admin.client.KeycloakConfiguration;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.OAuth2Constants;

import org.keycloak.representations.idm.ClientRepresentation;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class AdminClientApi {
    
    private static final Integer DEFAULT_CONNPOOL_SIZE = 5;
    private static final Integer DEFAULT_MAX_CONN_PER_ROUTE = 100;
    private static final String  SAML_PROTOCOL = "saml";
    private static final Pattern CLIENT_NAME_REGEX = Pattern.compile("^jans_saml_([a-zA-Z0-9\\-]+)$");
    private static final Logger log = LoggerFactory.getLogger(AdminClientApi.class);

    private Keycloak keycloak;

    private AdminClientApi(Keycloak keycloak) {

        this.keycloak = keycloak;
    }

    public List<ManagedSamlClient> findAllManagedSamlClients(String realmname) {

        try {
            RealmResource realmresource = realmByName(realmname);
            ClientsResource clientsresource = realmresource.clients();
            List<ClientRepresentation> clientsrep = clientsresource.findAll();
            log.debug("Clients from realm count : {}",clientsrep.size());
            return clientsrep.stream()
                .filter(AdminClientApi::isManagedClientRepresentation)
                .map(AdminClientApi::toManagedSamlClient)
                .collect(Collectors.toList());
        }catch(Exception e) {
            throw new AdminClientApiError("Could not get managed clients",e);
        }
    }

    public void deleteManagedSamlClient(String realmname, ManagedSamlClient client) {

        try {
            RealmResource realmresource = realmByName(realmname);
            ClientsResource clientsresource = realmresource.clients();
            ClientResource clientresource = clientsresource.get(client.keycloakId());
            if(clientresource != null) {
                clientresource.remove();
            }
        }catch(Exception e) {
            throw new AdminClientApiError("Could not delete managed client",e);
        }
    }

    private RealmResource realmByName(String realmname) {

        return keycloak.realm(realmname);
    }

    public static final AdminClientApi createInstance(KeycloakConfiguration kcConfig) {

        try {
            Keycloak kc = createKeycloakInstance(kcConfig);
            //get server info to force authentication verification
            //to occur
            kc.serverInfo().getInfo();
            return new AdminClientApi(kc);
        }catch(IllegalStateException e) {
            throw new KeycloakConfigurationError("Could not create keycloak instance",e);
        }
    }

    private static final Keycloak createKeycloakInstance(KeycloakConfiguration kcConfig) throws IllegalStateException {

        
        return KeycloakBuilder.builder()
            .serverUrl(kcConfig.serverUrl())
            .realm(kcConfig.realm())
            .username(kcConfig.username())
            .password(kcConfig.password())
            .clientId(kcConfig.clientId())
            .grantType(OAuth2Constants.PASSWORD)
            .resteasyClient(createResteasyClient(kcConfig))
            .build();
    }

    private static final Client createResteasyClient(KeycloakConfiguration kcConfig) {

        Integer effectivecpsize = DEFAULT_CONNPOOL_SIZE;
        effectivecpsize = ((kcConfig.connPoolSize() == null|| kcConfig.connPoolSize() == 0))?DEFAULT_CONNPOOL_SIZE:kcConfig.connPoolSize();

        PoolingHttpClientConnectionManager connmgr = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connmgr).build();
        connmgr.setMaxTotal(effectivecpsize);
        connmgr.setDefaultMaxPerRoute(DEFAULT_MAX_CONN_PER_ROUTE);

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        return ( (ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
    }

    private static boolean isManagedClientRepresentation(ClientRepresentation client) {

        log.debug("Protocol: {} / Name: {}",client.getProtocol(),client.getName());
        if(!SAML_PROTOCOL.equalsIgnoreCase(client.getProtocol())) {
            log.debug("Protocol does not match");
            return false;
        }

        Matcher matcher = CLIENT_NAME_REGEX.matcher(client.getName());
        boolean res = matcher.matches();
        log.debug("Matches: {}",res);
        return res;
    }

    private static ManagedSamlClient toManagedSamlClient(ClientRepresentation client) {

        Matcher matcher = CLIENT_NAME_REGEX.matcher(client.getName());
        if(matcher.matches() == true) {
            return new ManagedSamlClient(client,matcher.group(1));
        }
        return null;
    }

    
}
