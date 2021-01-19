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
import io.jans.configapi.auth.ConfigApiProtectionCache;
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
    UmaService umaService;

    @Inject
    EncryptionService encryptionService;

    public String getClientId() {
        return this.configurationFactory.getApiClientId();
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

    public List<Scope> getResourceScopeList(String method, String path) {
        log.trace(" AuthUtil::getResourceScopeList() method = "
                + method + " , path = " + path + "\n");

        // Verify in cache
        Map<String, List<Scope>> resources = ConfigApiProtectionCache.getAllResources();

        // Filter paths based on resource name
        Set<String> keys = resources.keySet();
        List<String> filteredPaths = keys.stream().filter(k -> k.contains(path)).collect(Collectors.toList());

        if (filteredPaths == null || filteredPaths.isEmpty()) {
            throw new WebApplicationException("No matching resource found .",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<Scope> scopeList = null;
        for (String key : filteredPaths) {
            String[] result = key.split(":::");
            if (result != null && result.length > 1) {
                String httpmethod = result[0];
                String pathUrl = result[1];
                log.debug(" AuthUtil::getResourceScopeList() - httpmethod = " + httpmethod + " , pathUrl = " + pathUrl);
                if (pathUrl != null && pathUrl.contains(path)) {
                    // Matching url
                    log.debug(" AuthUtil::getResourceScopeList() - Matching url, path = " + path + " , pathUrl = " + pathUrl);

                    // Verify Method
                    if (httpmethod.contains(method)) {
                    	scopeList = ConfigApiProtectionCache.getResource(key);
                        log.debug(" AuthUtil::getResourceScopeList() - Matching scopeList =" + scopeList);
                        break;
                    }
                    
                }

            }

        }
     
        return scopeList;
    }

    public List<String> getRequestedScopes(String path) {
        List<Scope> scopeList = ConfigApiProtectionCache.getResource(path);
        log.trace("getRequestedScopes() - scopeList = "+scopeList);
        List<String> scopeStrList = new ArrayList();
        if (scopeList != null && scopeList.size() > 0) {
            for (Scope s : scopeList) {
            	scopeStrList.add(s.getId());
            }
        }
        log.trace("\n\n\n AuthUtil:::getRequestedScopes() - scopeStrList = "+scopeStrList+"\n\n\n");
        return scopeStrList;
    }
    
    public List<String> getRequestedScopes(String method, String path) {
        log.trace("getRequestedScopes() - method = "+method+" , path = "+path);
        List<Scope> scopeList = this.getResourceScopeList(method,path);
        log.trace("\n\n\n AuthUtil:::getRequestedScopes() - scopeList = "+scopeList+"\n\n\n");
        List<String> scopeStrList = new ArrayList();
        if (scopeList != null && scopeList.size() > 0) {
            for (Scope s : scopeList) {
            	scopeStrList.add(s.getId());
            }
        }
        log.trace("\n\n\n AuthUtil:::getRequestedScopes() - scopeStrList = "+scopeStrList+"\n\n\n");
        return scopeStrList;
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

    public Token requestAccessToken(final String tokenUrl, final String clientId, final List<String> scopes)
            throws Exception {
        log.trace("RequestAccessToken() - tokenUrl = " + tokenUrl + " ,clientId = " + clientId + " ,scopes = " + scopes
                + "\n");

        // Get clientSecret
        String clientSecret = this.getClientDecryptPassword(clientId);

        // distinct scopes
        Set<String> scopesSet = new HashSet<String>(scopes);

        String scope = ScopeType.OPENID.getValue();
        if (scopesSet != null && scopesSet.size() > 0) {
            for (String s : scopes) {
                scope = scope + " " + s;
            }
        }
        log.trace("\n\n\n RequestAccessToken() - scope = "+scope);
        TokenResponse tokenResponse = AuthClientFactory.requestAccessToken(tokenUrl, clientId, clientSecret, scope);
        if (tokenResponse != null) {
            log.debug(" tokenScope: {} = ", tokenResponse.getScope());
            final String accessToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            if (Util.allNotBlank(accessToken)) {
                return new Token(null, null, accessToken, ScopeType.OPENID.getValue(), expiresIn);
            }
        }
        return null;
    }

    public Token requestPat(final String tokenUrl, final String clientId, final ScopeType scopeType,
            final List<String> scopes) throws Exception {
        return request(tokenUrl, clientId, this.getClientDecryptPassword(clientId), scopeType, scopes);
    }

    public Token request(final String tokenUrl, final String clientId, final String clientSecret, ScopeType scopeType,
            List<String> scopes) throws Exception {

        String scope = scopeType.getValue();
        if (scopes != null && scopes.size() > 0) {
            for (String s : scopes) {
                scope = scope.trim() + " " + s;
            }
        }

        TokenResponse tokenResponse = AuthClientFactory.patRequest(tokenUrl, clientId, clientSecret, scope);

        if (tokenResponse != null) {
            log.debug(" tokenScope: {} = ", tokenResponse.getScope());
            final String patToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
            }
        }
        return null;
    }

    public TokenResponse requestRpt(final String clientId, final String resourceId, final List<String> scopes,
            Token patToken) throws Exception {
        log.trace(" RPT request parameters, clientId: {}, resourceId: {}, scopes: {}, patToken: {} ", clientId,
                resourceId, scopes, patToken);

        // Get client
        // Client client = getClient(clientId);

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

        PermissionTicket permissionTicket = umaService.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(umaPermission));

        if (permissionTicket == null) {
            return null;
        }
        log.debug(" permissionTicket: {} = ", permissionTicket.toString());

        // Register RPT token
        TokenResponse tokenResponse = null;
        try {

            tokenResponse = AuthClientFactory.requestRpt(this.getTokenUrl(), clientId, this.getClientDecryptPassword(clientId), scopes,
                    permissionTicket.getTicket(), GrantType.OXAUTH_UMA_TICKET,
                    AuthenticationMethod.CLIENT_SECRET_BASIC);

            log.trace(" Rpt Token Response  = " + tokenResponse);
            if (tokenResponse != null) {
                log.debug(" Rpt Token Response Scope(): {} = ", tokenResponse.getScope());
            }

        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }

        return tokenResponse;
    }

    public void assignAllScope(final String clientId) {
        log.trace(" AssignAllScope to clientId = " + clientId + "\n");

        // Get Client
        Client client = this.clientService.getClientByInum(clientId);
        if (client != null) {

            // Prepare scope array
            List<String> scopes = getScopeWithDn(getAllScopes());
            String[] scopeArray = this.getAllScopesArray(scopes);
            log.debug(" AllScope = " + Arrays.asList(scopeArray) + "\n");

            if (client != null) {
                // Assign scope
                client.setScopes(scopeArray);
                this.clientService.updateClient(client);
            }
        }
        client = this.clientService.getClientByInum(clientId);
        log.debug(" Verify scopes post assignment, clientId: {} , scopes: {}", clientId,
                Arrays.asList(client.getScopes()));
    }

    public List<String> getAllScopes() {
        List<String> scopes = new ArrayList<String>();

        // Verify in cache
        Map<String, Scope> scopeMap = ConfigApiProtectionCache.getAllScopes();
        Set<String> keys = scopeMap.keySet();

        for (String id : keys) {
            Scope scope = ConfigApiProtectionCache.getScope(id);
            scopes.add(scope.getInum());
        }
        return scopes;
    }

    public String[] getAllScopesArray(List<String> scopes) {
        String[] scopeArray = null;

        if (scopes != null && !scopes.isEmpty()) {
            scopeArray = new String[scopes.size()];
            for (int i = 0; i < scopes.size(); i++) {
                scopeArray[i] = scopes.get(i);
            }
        }
        return scopeArray;
    }

    public List<String> getScopeWithDn(List<String> scopes) {
        List<String> scopeList = null;
        if (scopes != null && !scopes.isEmpty()) {
            scopeList = new ArrayList<String>();
            for (String id : scopes) {
                scopeList.add(this.scopeService.getDnForScope(id));
            }
        }
        return scopeList;
    }

}
