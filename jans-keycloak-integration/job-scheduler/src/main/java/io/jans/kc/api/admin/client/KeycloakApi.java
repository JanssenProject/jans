package io.jans.kc.api.admin.client;

import io.jans.kc.api.admin.client.KeycloakConfiguration;
import io.jans.kc.api.admin.client.model.AuthenticationFlow;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;
import io.jans.saml.metadata.model.EntityDescriptor;
import io.jans.saml.metadata.model.IndexedEndpoint;
import io.jans.saml.metadata.model.KeyDescriptor;
import io.jans.saml.metadata.model.SAMLBinding;
import io.jans.saml.metadata.model.SPSSODescriptor;
import io.jans.saml.metadata.model.ds.KeyInfo;
import io.jans.saml.metadata.model.ds.X509Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.OAuth2Constants;

import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class KeycloakApi {
    
    

    private static final Integer DEFAULT_CONNPOOL_SIZE = 5;
    private static final Integer DEFAULT_MAX_CONN_PER_ROUTE = 100;
    private static final String  SAML_PROTOCOL = "saml";
    private static final Pattern MANAGED_SAML_CLIENT_NAME_REGEX = Pattern.compile("^managed_saml_client_([a-zA-Z0-9\\-]+)$");
    private static final String  MANAGED_SAML_CLIENT_NAME_FORMAT = "managed_saml_client_%s";
    private static final String  MANAGED_SAML_CLIENT_DESC_FORMAT = "managed saml client. External #ref %s.";
    private static final String  BROWSER_AUTHN_FLOW_KEY = "browser";

    private static final Logger log = LoggerFactory.getLogger(KeycloakApi.class);

    private Keycloak keycloak;

    private KeycloakApi(Keycloak keycloak) {

        this.keycloak = keycloak;
    }

    public AuthenticationFlow getAuthenticationFlowFromAlias(String realmname, String alias) {

        try {
            RealmResource realmresource = realmByName(realmname);
            AuthenticationManagementResource authnmanagementresource = realmresource.flows();
            List<AuthenticationFlowRepresentation> flows = authnmanagementresource.getFlows();
            Optional<AuthenticationFlowRepresentation> authnflow = flows
                .stream()
                .filter((f)-> {
                    return f.getAlias()!=null && f.getAlias().equals(alias);
                }).findFirst();
            
            return (authnflow.isPresent()?new AuthenticationFlow(authnflow.get()):null);
        }catch(Exception e) {
            throw new KeycloakAdminClientApiError("Could not find authentication flow with alias " + alias,e);
        }
    }

    public List<ManagedSamlClient> findAllManagedSamlClients(String realmname) {

        try {
            RealmResource realmresource = realmByName(realmname);
            ClientsResource clientsresource = realmresource.clients();
            List<ClientRepresentation> clientsrep = clientsresource.findAll();
            log.debug("Clients from realm count : {}",clientsrep.size());
            return clientsrep.stream()
                .filter(KeycloakApi::isManagedSamlClientRepresentation)
                .map(KeycloakApi::toManagedSamlClient)
                .collect(Collectors.toList());
        }catch(Exception e) {
            throw new KeycloakAdminClientApiError("Could not get managed clients",e);
        }
    }

    public void deleteManagedSamlClient(String realmname,ManagedSamlClient client) {

        try {
            RealmResource realmresource = realmByName(realmname);
            ClientsResource clientsresource = realmresource.clients();
            ClientResource clientresource = clientsresource.get(client.keycloakId());
            if(clientresource != null) {
                clientresource.remove();
            }
        }catch(Exception e) {
            throw new KeycloakAdminClientApiError("Could not delete managed saml client",e);
        }
    }


    public ManagedSamlClient createManagedSamlClient(String realmname,String externalref,
        AuthenticationFlow browserflow, EntityDescriptor entitydesc) {

        try {
            RealmResource realmresource = realmByName(realmname);
            ClientsResource clientsresource = realmresource.clients();
            ClientRepresentation clientrep = new ClientRepresentation();
            ManagedSamlClient client = new ManagedSamlClient(clientrep,externalref);
            client.setName(managedSamlClientName(externalref));
            client.setDescription(managedSamlClientDescription(externalref));
            client.setClientId(entitydesc.getEntityId());

            //configure saml redirect uris
            configureSamlRedirectUris(entitydesc, client);

            //configure signing and encryption
            configureSamlEncryptionAndSigning(entitydesc,client);

            //configure keycloak authentication 
            configureKeycloakAuthentication(browserflow,client);

            Response response = clientsresource.create(clientrep);
            int code = response.getStatus();
            String body = response.readEntity(String.class);
            log.info("Create managed saml client: {} - {}",code,body);
            return client;
        }catch(Exception e) {
            throw new KeycloakAdminClientApiError("Could not create managed saml client",e);
        }
    }

    private void configureSamlRedirectUris(EntityDescriptor entitydescriptor, ManagedSamlClient client) {

        SPSSODescriptor spssodescriptor = entitydescriptor.getFirstSpssoDescriptor();
        if(spssodescriptor != null) {
            List<IndexedEndpoint> acs_endpoints = spssodescriptor.getAssertionConsumerServices();
            client.setSamlRedirectUris(
                acs_endpoints
                    .stream()
                    .filter((e) -> { return (e.getBinding() == SAMLBinding.HTTP_REDIRECT || e.getBinding() == SAMLBinding.HTTP_POST);})
                    .map((e) -> { return e.getLocation();})
                    .toList());
            boolean has_no_http_get_urls = acs_endpoints.stream().filter((e) -> { return e.getBinding() == SAMLBinding.HTTP_REDIRECT;}).count() == 0;
            client.samlForcePostBinding((acs_endpoints.size()>0 && has_no_http_get_urls));
        }
    }

    private void configureSamlEncryptionAndSigning(EntityDescriptor entitydescriptor, ManagedSamlClient client) {

        SPSSODescriptor spssodescriptor = entitydescriptor.getFirstSpssoDescriptor();
        if(spssodescriptor!=null) {
            client.samlClientSignatureRequired(spssodescriptor.getAuthnRequestsSigned());
            client.samlSignAssertions(spssodescriptor.getWantAssertionsSigned());

            List<KeyDescriptor> signingkeys = spssodescriptor.getSigningKeys();
            if(!signingkeys.isEmpty()) {
                configureSamlSigningKey(signingkeys.get(0),client);
            }

            List<KeyDescriptor> encryptionkeys = spssodescriptor.getEncryptionKeys();
            if(!encryptionkeys.isEmpty()) {
                configureSamlEncryptionKey(encryptionkeys.get(0),client);
            }
        }
    }

    private void configureSamlSigningKey(KeyDescriptor keydescriptor, ManagedSamlClient client) {

        KeyInfo keyinfo = keydescriptor.getKeyInfo();
        List<X509Data> certdata = keyinfo.getDatalist();
        if(!certdata.isEmpty()) {
            client.samlClientSignatureRequired(true);
            client.samlClientSigningCertificate(certdata.get(0).getFirstX509Certificate());
        }
    }

    private void configureSamlEncryptionKey(KeyDescriptor keydescriptor, ManagedSamlClient client) {

        KeyInfo keyinfo = keydescriptor.getKeyInfo();
        List<X509Data> certdata = keyinfo.getDatalist();
        if(!certdata.isEmpty()) {
            
            client.samlClientEncryptionCertificate(certdata.get(0).getFirstX509Certificate());
        }
    }

    private void configureKeycloakAuthentication(final AuthenticationFlow browserflow, ManagedSamlClient client) {

        client.setBrowserFlow(browserflow.getId());
    }

    private Optional<AuthenticationFlowRepresentation> authnFlowFromAlias(RealmResource realm, String flowalias) {

        AuthenticationManagementResource authn = realm.flows();
        List<AuthenticationFlowRepresentation> flows = authn.getFlows();
        return flows
            .stream()
            .filter((f)-> { return f.getAlias().equalsIgnoreCase(flowalias);})
            .findFirst();
    }

    private RealmResource realmByName(String realmname) {

        return keycloak.realm(realmname);
    }
    
    private Optional<AuthenticationFlowRepresentation> authnFlowByName(String realmname, String flowname) {

        RealmResource realm = keycloak.realm(realmname);
        if(realm == null) {
            return null;
        }
        AuthenticationManagementResource authnmanagement = realm.flows();
        List<AuthenticationFlowRepresentation> realmflows = authnmanagement.getFlows();
        return realmflows.stream() .filter((f)-> {
                return f.getAlias().equalsIgnoreCase(flowname);
            }).findFirst();
    }

    public static final KeycloakApi createInstance(KeycloakConfiguration kcConfig) {

        try {
            Keycloak kc = createKeycloakInstance(kcConfig);
            //get server info to force authentication verification
            //to occur
            kc.serverInfo().getInfo();
            return new KeycloakApi(kc);
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

    private static boolean isManagedSamlClientRepresentation(ClientRepresentation client) {

        if(!SAML_PROTOCOL.equalsIgnoreCase(client.getProtocol())) {
            return false;
        }
        Matcher matcher = MANAGED_SAML_CLIENT_NAME_REGEX.matcher(client.getName());
        return matcher.matches();
    }

    private static ManagedSamlClient toManagedSamlClient(ClientRepresentation client) {

        Matcher matcher = MANAGED_SAML_CLIENT_NAME_REGEX.matcher(client.getName());
        if(matcher.matches() == true) {
            return new ManagedSamlClient(client,matcher.group(1));
        }
        return null;
    }

    private static String managedSamlClientName(final String externalclientref) {

        return String.format(MANAGED_SAML_CLIENT_NAME_FORMAT,externalclientref);
    }

    private static String managedSamlClientDescription(final String externalclientref) {
        
        return String.format(MANAGED_SAML_CLIENT_DESC_FORMAT,externalclientref);
    }

    private static Map<String,String> authnFlowBindingOverrides(AuthenticationFlowRepresentation browserflow) {

        return new HashMap<String,String>() {{
            put(BROWSER_AUTHN_FLOW_KEY,browserflow.getId());
        }};
    }


}
