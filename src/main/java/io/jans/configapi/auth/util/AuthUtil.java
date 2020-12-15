package io.jans.configapi.auth.util;

import com.google.common.base.Preconditions;

import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.auth.UmaResourceProtectionCache;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.auth.client.UmaClient;
import io.jans.configapi.auth.service.UmaService;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.service.ClientService;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class AuthUtil {

    @Inject
    Logger log;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    ConfigurationService configurationService;

    @Inject
    ClientService clientService;

    @Inject
    ScopeService scopeService;

    @Inject
    UmaResourceProtectionCache resourceProtectionCache;

    @Inject
    UmaService umaService;

    @Inject
    EncryptionService encryptionService;

    @PostConstruct
    public void init() throws Exception {
        // Create clients if needed
        createClientIfNeeded(); // Todo: Uncomment later - ???

        // If test mode then create create token with scopes
        System.out.println("\n AuthUtil::init() - Entry - isTestMode() = " + isTestMode() + "\n");
    }

    public String getClientId() {
        return this.configurationFactory.getApiClientId();
    }

    public String getApiProtectionType() {
        return this.configurationFactory.getApiProtectionType();
    }

    private String getTestClientId() {
        return this.configurationFactory.getApiTestClientId();
    }

    public boolean isTestMode() {
        return configurationFactory.getAppExecutionMode() != null
                && "TEST".equalsIgnoreCase(configurationFactory.getAppExecutionMode());
    }

    public String getTokenUrl() {
        return this.configurationService.find().getTokenEndpoint();
    }
    
    public String getTokenRevocationEndpoint() {
        return this.configurationService.find().getTokenRevocationEndpoint();
    }

    public Client getClient(String clientId) {
        return clientService.getClientByInum(clientId);
    }

    public String getClientPassword(String clientId) {
        System.out.println("\n $$$$$$$$$$$$$$$$$ AuthUtil::getClientPassword() - clientId = " + clientId + "\n");
        return this.getClient(clientId).getClientSecret();
    }

    public String getClientDecryptPassword(String clientId) {
        return decryptPassword(getClientPassword(clientId));
    }

    public String decryptPassword(String clientPassword) {
        String decryptedPassword = null;
        if (clientPassword != null) {
            try {
                decryptedPassword = encryptionService.decrypt(clientPassword);
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt password", ex);
            }
        }
        return decryptedPassword;
    }

    public String encryptPassword(String clientPassword) {
        String encryptedPassword = null;
        if (clientPassword != null) {
            try {
                encryptedPassword = encryptionService.encrypt(clientPassword);
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt password", ex);
            }
        }
        return encryptedPassword;
    }

    public List<String> getRequestedScopes(String path) {
        UmaResource resource = resourceProtectionCache.getUmaResource(path);
        log.debug(" resource = " + resource);
        return resource.getScopes();
    }

    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        
        Class<?> resourceClass = resourceInfo.getResourceClass();
        ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
        List<String> scopes = new ArrayList<String>();
        if (typeAnnotation == null) {
            addMethodScopes(resourceInfo, scopes);
        } else {
            scopes.addAll(Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
            addMethodScopes(resourceInfo, scopes);
        }
        return scopes;
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        Set<String> authScopeSet = new HashSet<String>(authScopes);
        Set<String> resourceScopeSet = new HashSet<String>(resourceScopes);
        return authScopeSet.containsAll(resourceScopeSet);
    }

    private void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
        if (methodAnnotation != null) {
            scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
        }
    }

    public void  revokeToken( final String token) throws Exception {
        revokeToken( this.getTestClientId(), token);
    }
    
    public void  revokeToken( final String clientId, final String token) throws Exception {
        System.out.println("\n AuthUtil::revokeToken() - clientId = "+clientId+" ,token = " + token + "\n");       
       
        // Get clientSecret
        String clientSecret = this.getClientPassword(clientId);
        clientSecret = "test1234"; // Todo: Remove later - ???
        TokenResponse tokenResponse = AuthClientFactory.revokeToken(this.getTokenRevocationEndpoint(), clientId, clientSecret, token);
       

    }
    
    public Token requestAccessToken(final String tokenUrl, final String clientId, final List<String> scopes)
            throws Exception {
        System.out.println("\n AuthUtil::requestAccessToken() - tokenUrl = " + tokenUrl + " ,clientId = " + clientId
                + " ,scopes = " + scopes + "\n");
        
        // Get clientSecret
        String clientSecret = this.getClientPassword(clientId);
        clientSecret = "test1234"; // Todo: Remove later - ???
        
        // distinct scopes
        Set<String> scopesSet = new HashSet<String>(scopes);

        String scope = ScopeType.OPENID.getValue();
        if (scopesSet != null && scopesSet.size() > 0) {
            for (String s : scopes) {
                scope = scope + " " + s;
            }
        }

        TokenResponse tokenResponse = AuthClientFactory.requestAccessToken(tokenUrl, clientId, clientSecret, scope);
        if (tokenResponse != null) {
            System.out
            .println("\n AuthUtil::requestAccessToken() - tokenResponse.getScope() = " + tokenResponse.getScope() + "\n");
            final String accessToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            if (Util.allNotBlank(accessToken)) {
                return new Token(null, null, accessToken, ScopeType.OPENID.getValue(), expiresIn);
            }
        }
        return null;
    }

    public Token requestPat(final String tokenUrl, final String clientId, final List<String> scopes) throws Exception {
        // return request(tokenUrl, clientId, this.getClientDecryptPassword(clientId),
        // scopes);
        return request(tokenUrl, clientId, "test1234", scopes);
    }

    public Token request(final String tokenUrl, final String clientId, final String clientSecret,
            final List<String> scopes) throws Exception {
        return request(tokenUrl, clientId, clientSecret, UmaScopeType.PROTECTION, scopes);
    }

    public static Token request(final String tokenUrl, final String clientId, final String clientSecret,
            UmaScopeType scopeType, List<String> scopes) throws Exception {

        String scope = scopeType.getValue();
        if (scopes != null && scopes.size() > 0) {
            for (String s : scopes) {
                scope = scope.trim() + " " + s;
            }
        }
        System.out.println("\n AuthUtil::request() - scope = " + scope + "\n");
        TokenResponse tokenResponse = AuthClientFactory.patRequest(tokenUrl, clientId, clientSecret, scope);
        // TokenResponse tokenResponse = AuthClientFactory.patRequest_2(tokenUrl,
        // clientId, clientSecret, scope);
        System.out.println("\n AuthUtil::request() - tokenResponse.toString() = " + tokenResponse.toString() + "\n");
        if (tokenResponse != null) {

            final String patToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            System.out
                    .println("\n AuthUtil::request() - tokenResponse.getScope() = " + tokenResponse.getScope() + "\n");
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
            }
        }
        return null;
    }

    public TokenResponse requestRpt(final String clientId, final String resourceId, final List<String> scopes,
            Token patToken) throws Exception {
        System.out.println("\n $$$$$$$$$$$$$$$$$ AuthUtil::requestRpt() - clientId = " + clientId + " ,  resourceId = "
                + resourceId + " ,  scopes = " + scopes + " , patToken  = " + patToken + " \n");

        // Get client
        Client client = getClient(clientId);
        System.out.println("\n AuthUtil::requestPat() - client = " + client + " , client.getClientId() = "
                + client.getClientId() + " , client.getClientSecret() = " + client.getClientSecret() + "\n");

        // Generate Token with required scope for testing
        String scope = UmaScopeType.PROTECTION.getValue();
        if (scopes != null && scopes.size() > 0) {
            for (String s : scopes) {
                scope = scope + " " + s;
            }
        }

        // Register Permission
        UmaPermission umaPermission = new UmaPermission();
        umaPermission.setResourceId(resourceId);
        umaPermission.setScopes(scopes);
        System.out.println("\n AuthUtil::requestRpt() - UmaPermission = " + umaPermission + "\n");
        PermissionTicket permissionTicket = umaService.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(umaPermission));

        System.out.println("\n AuthUtil::requestRpt() FINAL - permissionTicket = " + permissionTicket + "\n");
        if (permissionTicket == null) {
            return null;
        }

        // Register RPT token
        TokenResponse tokenResponse = null;
        try {
            // rptStatusResponse =
            // this.getUmaRptIntrospectionService().requestRptStatus(authorization,rptToken,"");
            /*
             * tokenResponse = AuthClientFactory.requestRpt(this.getTokenUrl(),
             * client.getClientId(), this.decryptPassword(client.getClientSecret()), scopes,
             * permissionTicket.getTicket(), GrantType.OXAUTH_UMA_TICKET,
             * AuthenticationMethod.CLIENT_SECRET_BASIC);
             */
            // Error - 14:40:40 ERROR [io.ja.co.au.ut.AuthUtil] (executor-thread-2) Failed
            // to decrypt password:
            // io.jans.util.security.StringEncrypter$EncryptionException:
            // javax.crypto.BadPaddingException: Given final block not properly padded. Such
            // issues can arise if a bad key is used during decryption.
            tokenResponse = AuthClientFactory.requestRpt(this.getTokenUrl(), client.getClientId(), "test1234", scopes,
                    permissionTicket.getTicket(), GrantType.OXAUTH_UMA_TICKET,
                    AuthenticationMethod.CLIENT_SECRET_BASIC);

            System.out.println("\n AuthUtil::requestRpt() - tokenResponse  = " + tokenResponse);
            System.out.println("\n AuthUtil::requestRpt() - tokenResponse.toString()  = " + tokenResponse.toString());
            System.out.println(
                    "\n\n AuthUtil::requestRpt() - okenResponse.getAccessToken()  = " + tokenResponse.getAccessToken());
        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }

        return tokenResponse;
    }

    public String testPrep(ResourceInfo resourceInfo, String method, String path) throws Exception {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::testPrep() - Entry -  method = " + method + " ,path = "
                + path + "\n");
        Token token = null;
        
        //Assign scopes t client
        assignScope(this.getTestClientId(),this.getRequestedScopes(resourceInfo));
        
        if (ApiConstants.PROTECTION_TYPE_OAUTH2.equals(this.getApiProtectionType())) {
            // token = requestPat(getTokenUrl(), this.getTestClientId(),
            // this.getRequestedScopes(resourceInfo));
            token = requestAccessToken(getTokenUrl(), this.getTestClientId(), this.getRequestedScopes(resourceInfo));
        } else {
            token = registerTestClientRptTicket(resourceInfo, method, path);
        }
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::testPrep() - token = " + token + "\n");
        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }

    public Token registerTestClientRptTicket(ResourceInfo resourceInfo, String method, String path) throws Exception {
        System.out.println(
                "\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - resourceInfo = " + resourceInfo + "\n");

        List<String> scopes = this.getRequestedScopes(path);
        System.out.println("\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - scopes = " + scopes + "\n");

        // Get Pat
        Token patToken = requestPat(this.getTokenUrl(), this.getClientId(), scopes);
        System.out.println(
                "\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - patToken = " + patToken + "\n");

        UmaResource umaResource = this.getUmaResource(resourceInfo, method, path);
        if (patToken != null && umaResource != null) {

            // Register RPT token
            TokenResponse tokenResponse = this.requestRpt(this.getClientId(), umaResource.getId(), scopes, patToken);
            System.out.println(
                    "\n AuthUtil::registerTestClientRptTicket() FINAL - tokenResponse = " + tokenResponse + "\n");

            if (tokenResponse != null) {
                final String token = tokenResponse.getAccessToken();
                final Integer expiresIn = tokenResponse.getExpiresIn();
                if (Util.allNotBlank(token)) {
                    return new Token(null, null, token, UmaScopeType.PROTECTION.getValue(), expiresIn);
                }
            }
        }
        return null;
    }

    private void createClientIfNeeded() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("api-client.json");

        ClientList clientList = Jackson.createJsonMapper().readValue(inputStream, ClientList.class);
        System.out.println(" \n AuthUtil::createClientIfNeeded() - clientList = " + clientList + "\n");
        List<Client> clients = clientList.getClients();

        System.out.println(" \n AuthUtil::createClientIfNeeded() - clients = " + clients + "\n");

        Preconditions.checkNotNull(clients, "Config Api Client list cannot be null !!!");

        // Create client
        for (Client clt : clients) {
            System.out.println(" \n AuthUtil::createClientIfNeeded() - clt = " + clt + "\n");
            // Check if exists
            Client client = null;

            try {
                client = this.clientService.getClientByInum(clt.getClientId());
                System.out.println(" \n AuthUtil::createClientIfNeeded() - Verify client = " + client + "\n");

            } catch (Exception ex) {
                log.error("Error while searching client " + ex);
            }

            if (client == null) {
                // Create client
                clt.setDn(clientService.getDnForClient(clt.getClientId()));
                clt.setClientSecret(encryptPassword("test1234"));
                System.out.println(" \n AuthUtil::createClientIfNeeded() - Create clt = " + clt + "\n");
                this.clientService.addClient(clt);
            } else {
                clt.setDn(clientService.getDnForClient(clt.getClientId()));
                clt.setClientSecret(encryptPassword("test1234"));
                System.out.println(" \n AuthUtil::createClientIfNeeded() - Update clt = " + clt + "\n");
                this.clientService.updateClient(clt);
            }

            client = this.clientService.getClientByInum(clt.getClientId());
            System.out.println(
                    " \n @@@@@@@@@@@@@@@@@@@@@@@ AuthUtil::createClientIfNeeded() - Final client = " + client + "\n");

        }

    }

    public UmaResource getUmaResource(ResourceInfo resourceInfo, String method, String path) {
        log.trace(" AuthUtil::getUmaResource() - resourceInfo = " + resourceInfo
                + " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + " , method = "
                + method + " , path = " + path + "\n");

        // Verify in cache
        Map<String, UmaResource> resources = UmaResourceProtectionCache.getAllUmaResources();

        // Filter paths based on resource name
        Set<String> keys = resources.keySet();
        List<String> filteredPaths = keys.stream().filter(k -> k.contains(path)).collect(Collectors.toList());

        if (filteredPaths == null || filteredPaths.isEmpty()) {
            throw new WebApplicationException("No matching resource found .",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        UmaResource umaResource = null;
        for (String key : filteredPaths) {
            String[] result = key.split(":::");
            if (result != null && result.length > 1) {
                String httpmethod = result[0];
                String pathUrl = result[1];
                log.debug(" AuthUtil::getUmaResource() - httpmethod = " + httpmethod + " , pathUrl = " + pathUrl);
                if (path.equals(pathUrl)) {
                    // Matching url
                    log.debug(" AuthUtil::getUmaResource() - Matching url, path = " + path + " , pathUrl = " + pathUrl);

                    // Verify Method
                    if (httpmethod.contains(method)) {
                        umaResource = UmaResourceProtectionCache.getUmaResource(key);
                        log.debug(" AuthUtil::getUmaResource() - Matching umaResource =" + umaResource);
                        break;
                    }

                }

            }

        }
        return umaResource;
    }
    
    public void assignAllScope(final String clientId) {
        System.out.println("\n AuthUtil::assignAllScope() - Entry - clientId = "+clientId+"\n");        
        
        //Get Client
        Client client = this.clientService.getClientByInum(clientId);
        if(client != null) {
            
            System.out.println(" \n AuthUtil::assignScope() -  client.getAllScope() = " + client.getClientId() + ", Arrays.asList(client.getScopes()) = "+Arrays.asList(client.getScopes())+"\n");
            if (client != null) {
                //Assign scope 
                client.setScopes(addScopes(client,geScopeWithDn(getAllScopes())));
                this.clientService.updateClient(client);          
            }
        }
        client = this.clientService.getClientByInum(clientId);
        System.out.println(" \n AuthUtil::assignScope() - Final -  client.getAllScope() = " + client.getClientId() +", Arrays.asList(client.getScopes()) = "+Arrays.asList(client.getScopes())+"\n");     
    }
    
    public List<String> getAllScopes(){
        List<String> scopes = new ArrayList<String>();
        
         // Verify in cache
         Map<String, Scope> scopeMap = UmaResourceProtectionCache.getAllScopes();
        
         Set<String> keys = scopeMap.keySet();
         
         for (String id : keys) {
             Scope scope = UmaResourceProtectionCache.getScope(id);
             scopes.add(scope.getId());            
        }
        return scopes;         
    }

    public void assignScope(final String clientId, final List<String> scopes) {
        System.out.println("\n AuthUtil::assignScope() - Entry - clientId = "+clientId+" , scopes = "+scopes);        
        
        //Get Client
        Client client = this.clientService.getClientByInum(clientId);
        if(client != null) {
            
            System.out.println(" \n AuthUtil::assignScope() -  client.getClientId() = " + client.getClientId() + ", Arrays.asList(client.getScopes()) = "+Arrays.asList(client.getScopes())+"\n");
            if (client != null) {
                //Assign scope 
                client.setScopes(addScopes(client,geScopeWithDn(scopes)));
                this.clientService.updateClient(client);          
            }
        }
        client = this.clientService.getClientByInum(clientId);
        System.out.println(" \n AuthUtil::assignScope() - Final -  client.getClientId() = " + client.getClientId() +", Arrays.asList(client.getScopes()) = "+Arrays.asList(client.getScopes())+"\n");
     
    }

    public List<String> geScopeWithDn(List<String> scopes) {
        List<String> scopeList = null;
        if (scopes != null && scopes.size() > 0) {
            scopeList = new ArrayList<String>();
            for (String id : scopes) {
                List<Scope> searchedScope = this.scopeService.searchScopes(id, 1);
                if (searchedScope != null && searchedScope.size() > 0) {
                    for (int i=0;i<searchedScope.size();i++) {
                        Scope scope = searchedScope.get(i);
                        scopeList.add(this.scopeService.getDnForScope(scope.getInum()));
                    }
                }
            }
        }
        System.out.println("\n AuthUtil::geScopeWithDn() - Exit - scopeList = " + scopeList + "\n");
        return scopeList;
    }
    
    public String[] addScopes(Client client, List<String> scopes) {
   
        String[] clientScopes = client.getScopes();
       
        //distinct resources
        Set<String> scopeSet = new HashSet<String>(scopes);
        
        /*
        if(clientScopes.length>0) {
            for (int i=0;i<clientScopes.length;i++) {
                scopeSet.add(clientScopes[i]);
            }
        }*/
 
        scopes = new ArrayList<String>(scopeSet);
        
        String[] scopeArray = null;
        if(scopes!=null && !scopes.isEmpty()) {
            scopeArray = new String[scopes.size()];
            for (int i=0;i<scopes.size();i++) {
                scopeArray[i] = scopes.get(i);         
            }
        }
        
        System.out.println("\n AuthUtil::addScopes() - scopeArray = " + Arrays.toString(scopeArray)+"\n");
        return scopeArray;
    }

}
