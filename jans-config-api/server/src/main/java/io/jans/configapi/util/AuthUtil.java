package io.jans.configapi.util;

import com.unboundid.ldap.sdk.DN;
import io.jans.as.client.RevokeSessionResponse;
import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.model.configuration.AgamaConfiguration;
import io.jans.configapi.security.api.ApiProtectionCache;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

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
    EncryptionService encryptionService;

    public String getOpenIdConfigurationEndpoint() {
        return this.configurationService.find().getOpenIdConfigurationEndpoint();
    }

    public String getAuthOpenidConfigurationUrl() {
        return this.configurationFactory.getApiAppConfiguration().getAuthOpenidConfigurationUrl();
    }

    public String getIssuer() {
        return this.configurationService.find().getIssuer();
    }
    
    public String getIntrospectionEndpoint() {
        return configurationService.find().getIntrospectionEndpoint();
    }

    public String getTokenEndpoint() {
        return configurationService.find().getTokenEndpoint();
    }
    
    public String getEndSessionEndpoint() {
        return this.configurationService.find().getEndSessionEndpoint();
    }

    public String getServiceUrl(String url) {
        return this.getIssuer() + url;
    }

    public String getClientId() {
        return this.configurationFactory.getApiClientId();
    }
    
    public List<String> getUserExclusionAttributes() {
        return this.configurationFactory.getApiAppConfiguration().getUserExclusionAttributes();
    }
    
    public String getUserExclusionAttributesAsString() {
        List<String> excludedAttributes = getUserExclusionAttributes();
        return excludedAttributes == null ? null : excludedAttributes.stream().collect(Collectors.joining(","));
    }
    
    public List<String> getUserMandatoryAttributes() {
        return this.configurationFactory.getApiAppConfiguration().getUserMandatoryAttributes();
    }
    
    public AgamaConfiguration getAgamaConfiguration() {
        return this.configurationFactory.getApiAppConfiguration().getAgamaConfiguration();
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
        log.trace(" ResourceScopeList requested for method:{}, path:{}", method, path);

        // Verify in cache
        Map<String, List<Scope>> resources = ApiProtectionCache.getAllResources();

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
                log.trace(" Resource Scopes - httpmethod:{} , pathUrl:{} ", httpmethod, pathUrl);

                if (pathUrl != null && pathUrl.contains(path)) {
                    // Matching url
                    log.trace(" Matching url with path:{} , pathUrl:{} ", path, pathUrl);

                    // Verify Method
                    if (httpmethod.contains(method)) {
                        scopeList = ApiProtectionCache.getResourceScopes(key);
                        log.trace(" scopeList:{} for the method:{} ", scopeList, method);
                        break;
                    }
                }
            }
        }
        return scopeList;
    }

    public List<String> getAllResourceScopes() {
        Map<String, Scope> scopeMap = ApiProtectionCache.getAllScopes();
        log.trace("All Resource Scopes - scopeMap:{}", scopeMap);

        List<String> scopeStrList = null;
        if (scopeMap != null && !scopeMap.isEmpty()) {
            Set<String> scopeSet = scopeMap.keySet();
            scopeStrList = new ArrayList<>(scopeSet);
        }
        log.trace("All Resource Scopes - scopeStrList:{} ", scopeStrList);
        return scopeStrList;
    }

    public List<String> getRequestedScopes(String path) {
        List<Scope> scopeList = ApiProtectionCache.getResourceScopes(path);
        log.trace("Requested scopes:{} for path:{} ", scopeList, path);

        List<String> scopeStrList = new ArrayList<>();
        if (scopeList != null && !scopeList.isEmpty()) {
            for (Scope s : scopeList) {
                scopeStrList.add(s.getId());
            }
        }
        log.trace("Requested scopeStrList:{} for path:{}", scopeStrList, path);
        return scopeStrList;
    }

    public List<String> getRequestedScopes(String method, String path) {
        log.trace("Requested scopes for path:{} and method:{} ", path, method);
        List<Scope> scopeList = this.getResourceScopeList(method, path);
        log.trace("Requested scopeList:{} for path:{} and method:{} ", scopeList, path, method);
        List<String> scopeStrList = new ArrayList<>();
        if (scopeList != null && !scopeList.isEmpty()) {
            for (Scope s : scopeList) {
                scopeStrList.add(s.getId());
            }
        }
        log.trace("Final scopeStrList:{} for path:{} and method:{} ", scopeStrList, path, method);
        return scopeStrList;
    }

    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        log.trace("Requested scopes for resourceInfo:{} ", resourceInfo);
        Class<?> resourceClass = resourceInfo.getResourceClass();
        ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
        List<String> scopes = new ArrayList<>();
        if (typeAnnotation == null) {
            addMethodScopes(resourceInfo, scopes);
        } else {
            scopes.addAll(Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
            addMethodScopes(resourceInfo, scopes);
        }
        log.trace("Requested scopes:{} for resourceInfo:{} ", scopes, resourceInfo);
        return scopes;
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        Set<String> authScopeSet = new HashSet<>(authScopes);
        Set<String> resourceScopeSet = new HashSet<>(resourceScopes);
        return authScopeSet.containsAll(resourceScopeSet);
    }

    private void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
        if (methodAnnotation != null) {
            scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
        }
    }
    
    public String requestAccessToken(final String clientId, final List<String> scope) {
        log.info("Request for AccessToken - clientId:{}, scope:{} ", clientId, scope);
        String tokenUrl = getTokenEndpoint();
        Token token = getAccessToken(tokenUrl, clientId, scope);
        log.info("oAuth AccessToken response - token:{}", token);
        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }

    public Token getAccessToken(final String tokenUrl, final String clientId, final List<String> scopes) {
        log.debug("Access Token Request - tokenUrl:{}, clientId:{}, scopes:{}", tokenUrl, clientId, scopes);

        // Get clientSecret
        String clientSecret = this.getClientDecryptPassword(clientId);

        // distinct scopes
        Set<String> scopesSet = new HashSet<>(scopes);

        StringBuilder scope = new StringBuilder(ScopeType.OPENID.getValue());
        for (String s : scopesSet) {
            scope.append(" ").append(s);
        }

        log.debug("Scope required  - {}", scope);

        TokenResponse tokenResponse = AuthClientFactory.requestAccessToken(tokenUrl, clientId, clientSecret,
                scope.toString());
        if (tokenResponse != null) {

            log.debug("Token Response - tokenScope: {}, tokenAccessToken: {} ", tokenResponse.getScope(),
                    tokenResponse.getAccessToken());
            final String accessToken = tokenResponse.getAccessToken();
            final Integer expiresIn = tokenResponse.getExpiresIn();
            if (Util.allNotBlank(accessToken)) {
                return new Token(null, null, accessToken, ScopeType.OPENID.getValue(), expiresIn);
            }
        }
        return null;
    }

    public void assignAllScope(final String clientId) {
        log.trace("Client to be assigned all scope - {} ", clientId);

        // Get Client
        Client client = this.clientService.getClientByInum(clientId);
        if (client == null) {
            return;
        }

        // Prepare scope array
        List<String> scopes = getScopeWithDn(getAllScopes());
        String[] scopeArray = this.getAllScopesArray(scopes);
        log.debug(" scope to be assigned - {} ", Arrays.asList(scopeArray));
        // Assign scope
        client.setScopes(scopeArray);
        this.clientService.updateClient(client);
        client = this.clientService.getClientByInum(clientId);
        log.debug(" Verify scopes post assignment, clientId: {} , scopes: {}", clientId,
                Arrays.asList(client.getScopes()));
    }

    public List<String> getAllScopes() {
        List<String> scopes = new ArrayList<>();

        // Verify in cache
        Map<String, Scope> scopeMap = ApiProtectionCache.getAllScopes();
        Set<String> keys = scopeMap.keySet();

        for (String id : keys) {
            Scope scope = ApiProtectionCache.getScope(id);
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
            scopeList = new ArrayList<>();
            for (String id : scopes) {
                scopeList.add(this.scopeService.getDnForScope(id));
            }
        }
        return scopeList;
    }

    public boolean isValidIssuer(String issuer) {
        log.info("Is issuer:{} present in approvedIssuer list ? {} ", issuer,
                this.configurationFactory.getApiApprovedIssuer().contains(issuer));
        return this.configurationFactory.getApiApprovedIssuer().contains(issuer);
    }

    public List<String> getAuthSpecificScopeRequired(ResourceInfo resourceInfo) {
        log.debug("Fetch Auth server specific scope for resourceInfo:{} ", resourceInfo);

        // Get required oauth scopes for the endpoint
        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        log.debug(" resource:{} has these scopes:{} and configured exclusiveAuthScopes are {}", resourceInfo,
                resourceScopes, this.configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes());

        // Check if the path has any exclusiveAuthScopes requirement
        List<String> exclusiveAuthScopesToReq = new ArrayList<>();
        if (resourceScopes != null && !resourceScopes.isEmpty()
                && this.configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes() != null
                && !this.configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes().isEmpty()) {
            exclusiveAuthScopesToReq = resourceScopes.stream()
                    .filter(ele -> configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes().contains(ele))
                    .collect(Collectors.toList());
        }

        log.debug("Applicable exclusiveAuthScopes for resourceInfo:{} are {} ", resourceInfo, exclusiveAuthScopesToReq);
        return exclusiveAuthScopesToReq;
    }

    public List<String> findMissingElements(List<String> list1, List<String> list2) {
        return list1.stream().filter(e -> !list2.contains(e)).collect(Collectors.toList());
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        return CollectionUtils.isEqualCollection(list1, list2);
    }
    
    public boolean containsField(List<Field> allFields, String attribute) {
        log.debug("allFields:{},  attribute:{}, allFields.contains(attribute):{} ", allFields ,  attribute, allFields.stream().anyMatch(f -> f.getName().equals(attribute)));
         
        return allFields.stream().anyMatch(f -> f.getName().equals(attribute));
     }
     
     public List<Field> getAllFields(Class<?> type) {
         List<Field> allFields =  new ArrayList<>();
         allFields = getAllFields(allFields, type);
         log.debug("Fields:{} of type:{}  ", allFields, type);
                 
         return allFields;
     }
     
     public List<Field> getAllFields(List<Field> fields, Class<?> type) {
         log.debug("fields:{} of type:{} ", fields, type);
         fields.addAll(Arrays.asList(type.getDeclaredFields()));

         if (type.getSuperclass() != null) {
             getAllFields(fields, type.getSuperclass());
         }
         log.debug("Final fields:{} of type:{} ", fields, type);
         return fields;
     }
     
     public boolean isValidDn(String dn) {
         return isValidDn(dn, false);
     }

     
     public boolean isValidDn(String dn, boolean strictNameChecking) {
         return DN.isValidDN(dn, strictNameChecking);
     }

   
     public RevokeSessionResponse revokeSession(final String url,final String token, final String userId) {
         log.debug("Revoke session Request - url:{}, token:{}, userId:{}", url, token, userId);

        

         RevokeSessionResponse revokeSessionResponse = AuthClientFactory.revokeSession(url, token,userId);
         log.debug("revokeSessionResponse:{}",revokeSessionResponse);
         if (revokeSessionResponse != null) {

             log.debug("revokeSessionResponse.getEntity():{}, revokeSessionResponse.getStatus():{} ", revokeSessionResponse.getEntity(), revokeSessionResponse.getStatus());
           
            
         }
         return revokeSessionResponse;
     }

}
