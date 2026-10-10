package io.jans.configapi.util;

import com.unboundid.ldap.sdk.DN;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Scope;

import io.jans.configapi.model.configuration.AgamaConfiguration;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.model.configuration.DataFormatConversionConf;
import io.jans.configapi.model.configuration.PluginConf;
import io.jans.configapi.security.api.ApiProtectionCache;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.security.service.OpenIdService;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.model.role.RolePermissionMapping;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.service.ConfService;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.RolePermissionMappingService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.reflect.Field;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuthUtil {

    private static final Logger log = LoggerFactory.getLogger(AuthUtil.class);

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

    @Inject
    ConfService confService;
    
    @Inject 
    RolePermissionMappingService rolePermissionMappingService;
    
    @Inject
    AuthClientFactory authClientFactory;

    @Inject 
    OpenIdService openIdService;
    
    public String getOpenIdConfigurationEndpoint() {
        return this.configurationService.find().getOpenIdConfigurationEndpoint();
    }

    public String getAuthIssuerUrl() {
        return this.configurationFactory.getApiAppConfiguration().getAuthIssuerUrl();
    }
    
    public String getAuthOpenidConfigurationUrl() {
        return this.configurationFactory.getApiAppConfiguration().getAuthOpenidConfigurationUrl();
    }

    public AuditLogConf getAuditLogConf() {
        return this.configurationFactory.getApiAppConfiguration().getAuditLogConf();
    }

    public DataFormatConversionConf getDataFormatConversionConf() {
        return this.configurationFactory.getApiAppConfiguration().getDataFormatConversionConf();
    }

    public List<PluginConf> getPluginConf() {
        return this.configurationFactory.getApiAppConfiguration().getPlugins();
    }
    
    public String getUserRoleAttributeName() {
        return this.configurationFactory.getApiAppConfiguration().getUserRoleAttributeName();
    }
    
    public String getUserAdminRoleNameSubstring() {
        return this.configurationFactory.getApiAppConfiguration().getUserAdminRoleNameSubstring();
    }
    
    public boolean isUserRolePermissionValidationEnabled() {
        return this.configurationFactory.getApiAppConfiguration().isUserRolePermissionValidationEnabled();
    }

    public boolean isValidateUserInumInIntrospectionFlag() {
        return this.configurationFactory.getApiAppConfiguration().isValidateUserInumInIntrospectionFlag();
    }

    public List<String> getUserRolePermissionExcludedClients() {
        return this.configurationFactory.getApiAppConfiguration().getUserRolePermissionExcludedClients();
    }
    
    public List<String> getExcludedClients() {
        List<String> excludedClients = getUserRolePermissionExcludedClients();
        return excludedClients == null ? Collections.emptyList() : excludedClients;
    }
    
    public boolean isUserRolePermissionExcluded(String inum) {
        List<String> excludedClients = getExcludedClients();
        if(excludedClients!=null && !excludedClients.isEmpty() && excludedClients.contains(inum)) {
            return true;
        }else{
            return false;
        }
    }
    
    
    public boolean isFetchUserRoleInIntrospectionFlag() {
        return this.configurationFactory.getApiAppConfiguration().isFetchUserRoleInIntrospectionFlag();
    }
    
    public List<String> getSuperAdminScopes() {
        return this.configurationFactory.getApiAppConfiguration().getSuperAdminScopes();
    }

    public String getSuperAdminReadScope() {
        String readScope = null;
        if (getSuperAdminScopes() == null || getSuperAdminScopes().isEmpty()) {
            return readScope;
        }
        return getSuperAdminScopes().stream().filter(scope -> scope.contains("read")).findFirst().orElse(null);
    }

    public String getSuperAdminWriteScope() {
        String writeScope = null;
        if (getSuperAdminScopes() == null || getSuperAdminScopes().isEmpty()) {
            return writeScope;
        }
        return getSuperAdminScopes().stream().filter(scope -> scope.contains("write")).findFirst().orElse(null);
    }

    public String getSuperAdminDeleteScope() {
        String deleteScope = null;
        if (getSuperAdminScopes() == null || getSuperAdminScopes().isEmpty()) {
            return deleteScope;
        }
        return getSuperAdminScopes().stream().filter(scope -> scope.contains("delete")).findFirst().orElse(null);
    }

    public boolean hasSuperAdminReadScope(List<String> userScopes) {
        if (userScopes == null || userScopes.isEmpty()) {
            return false;
        }
        return userScopes.contains(getSuperAdminReadScope());
    }

    public boolean hasSuperAdminWriteScope(List<String> userScopes) {
        if (userScopes == null || userScopes.isEmpty()) {
            return false;
        }
        return userScopes.contains(getSuperAdminWriteScope());
    }

    public boolean hasSuperAdminDeleteScope(List<String> userScopes) {
        if (userScopes == null || userScopes.isEmpty()) {
            return false;
        }
        return userScopes.contains(getSuperAdminDeleteScope());
    }
    
    public boolean hasSuperAdminScope(List<String> userScopes, final String httpRequestMethod) {
        if (userScopes == null || userScopes.isEmpty() || StringUtils.isBlank(httpRequestMethod)) {
            return false;
        }

        switch (httpRequestMethod) {
        case ApiConstants.READ_REQUEST:
            return hasSuperAdminReadScope(userScopes) || hasSuperAdminWriteScope(userScopes);
    
        case ApiConstants.DELETE_REQUEST:
            return hasSuperAdminDeleteScope(userScopes) || hasSuperAdminWriteScope(userScopes);

        default:
            return hasSuperAdminWriteScope(userScopes);
   
        }
    }
    
    public String getIssuer() {
        return this.configurationService.find().getIssuer();
    }

    /**
     * Resolves an endpoint that config-api calls itself.
     *
     * <p>
     * When endpoint injection is enabled the injected URL is preferred, so that
     * config-api reaches the auth server over the address it was configured
     * with instead of the externally published one. Deployments where both run
     * in the same container or pod can then keep the call internal. Falls back
     * to the auth server configuration when injection is disabled or the
     * injected value is not set.
     * </p>
     *
     * @param injectedUrl  endpoint injected into the config-api configuration
     * @param publishedUrl endpoint published by the auth server
     * @return the endpoint to call
     */
    private String resolveEndpoint(String injectedUrl, String publishedUrl) {
        if (this.configurationFactory.getApiAppConfiguration().isEndpointInjectionEnabled()
                && StringUtils.isNotBlank(injectedUrl)) {
            log.debug("Using injected endpoint:{} instead of published endpoint:{}", injectedUrl, publishedUrl);
            return injectedUrl;
        }
        return publishedUrl;
    }

    /**
     * Rebases a URL published by the auth server onto the injected auth server
     * address.
     *
     * <p>
     * Used for endpoints that have no injected counterpart of their own. The
     * path, query and fragment of the published URL are preserved and only the
     * scheme and authority are replaced, which mirrors how the injected
     * endpoints are derived in the first place. Returns the published URL
     * unchanged when injection is disabled, when no injected issuer is
     * configured, or when either value cannot be parsed.
     * </p>
     *
     * @param publishedUrl URL published by the auth server
     * @return the URL to call
     */
    public String resolveAuthServerUrl(String publishedUrl) {
        String injectedIssuer = this.configurationFactory.getApiAppConfiguration().getAuthIssuerUrl();
        if (!this.configurationFactory.getApiAppConfiguration().isEndpointInjectionEnabled()
                || StringUtils.isBlank(injectedIssuer) || StringUtils.isBlank(publishedUrl)) {
            return publishedUrl;
        }

        try {
            URI published = new URI(publishedUrl);
            URI injected = new URI(injectedIssuer);

            if (StringUtils.isBlank(injected.getScheme()) || StringUtils.isBlank(injected.getRawAuthority())) {
                log.warn("Injected issuer:{} is not an absolute url, using published url:{}", injectedIssuer,
                        publishedUrl);
                return publishedUrl;
            }

            // Raw components are copied verbatim so that encoded delimiters in
            // the published url keep their meaning.
            StringBuilder resolved = new StringBuilder(injected.getScheme()).append("://")
                    .append(injected.getRawAuthority());
            if (StringUtils.isNotEmpty(published.getRawPath())) {
                resolved.append(published.getRawPath());
            }
            if (published.getRawQuery() != null) {
                resolved.append('?').append(published.getRawQuery());
            }
            if (published.getRawFragment() != null) {
                resolved.append('#').append(published.getRawFragment());
            }

            log.debug("Rebased published url:{} onto injected issuer:{} as:{}", publishedUrl, injectedIssuer,
                    resolved);
            return resolved.toString();
        } catch (URISyntaxException ex) {
            log.warn("Could not rebase published url:{} onto injected issuer:{}, using published url", publishedUrl,
                    injectedIssuer, ex);
            return publishedUrl;
        }
    }

    public String getIntrospectionEndpoint() {
        return resolveEndpoint(this.configurationFactory.getApiAppConfiguration().getAuthOpenidIntrospectionUrl(),
                configurationService.find().getIntrospectionEndpoint());
    }

    public String getTokenEndpoint() {
        return resolveEndpoint(this.configurationFactory.getApiAppConfiguration().getAuthOpenidTokenUrl(),
                configurationService.find().getTokenEndpoint());
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
        return resolveEndpoint(this.configurationFactory.getApiAppConfiguration().getAuthOpenidTokenUrl(),
                this.configurationService.find().getTokenEndpoint());
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

    public Map<ProtectionScopeType, List<String>> getRequestedScopes(ResourceInfo resourceInfo) {
        log.info("Requested scopes for resourceInfo:{} ", resourceInfo);

        Class<?> resourceClass = resourceInfo.getResourceClass();
        ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
        Map<ProtectionScopeType, List<String>> scopes = new HashMap<>();
        log.debug("Requested scopes for resourceClass:{}, typeAnnotation:{} ", resourceClass, typeAnnotation);

        if (typeAnnotation == null) {
            log.debug("Requested scopes for resourceClass:{}, typeAnnotation == null ", resourceClass);
            addMethodScopes(resourceInfo, scopes);
        } else {
            log.debug("Requested scopes for resourceClass:{}, typeAnnotation is not null ", resourceClass);
            scopes.put(ProtectionScopeType.SCOPE, Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
            scopes.put(ProtectionScopeType.GROUP, Stream.of(typeAnnotation.groupScopes()).collect(Collectors.toList()));
            scopes.put(ProtectionScopeType.SUPER, Stream.of(typeAnnotation.superScopes()).collect(Collectors.toList()));

            log.trace("ProtectionScopeType.SCOPE:{}, ProtectionScopeType.GROUP:{} ,  ProtectionScopeType.SUPER:{} ",
                    Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()),
                    Stream.of(typeAnnotation.groupScopes()).collect(Collectors.toList()),
                    Stream.of(typeAnnotation.superScopes()).collect(Collectors.toList()));

            log.debug("All scopes:{} ", scopes);
            addMethodScopes(resourceInfo, scopes);
        }
        log.info("*** Final Requested scopes:{} for resourceInfo:{} ", scopes, resourceInfo);
        return scopes;
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        log.info("Validate Scopes for authScopes:{}, resourceScopes:{} ", authScopes, resourceScopes);
        Set<String> authScopeSet = new HashSet<>(authScopes);
        Set<String> resourceScopeSet = new HashSet<>(resourceScopes);
        return authScopeSet.containsAll(resourceScopeSet);
    }

    private void addMethodScopes(ResourceInfo resourceInfo, Map<ProtectionScopeType, List<String>> scopes) {
        log.info("Method Scopes for resourceInfo:{}, scopes:{} ", resourceInfo, scopes);
        Method resourceMethod = resourceInfo.getResourceMethod();
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);

        if (methodAnnotation != null) {
            scopes.put(ProtectionScopeType.SCOPE, Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
            scopes.put(ProtectionScopeType.GROUP,
                    Stream.of(methodAnnotation.groupScopes()).collect(Collectors.toList()));
            scopes.put(ProtectionScopeType.SUPER,
                    Stream.of(methodAnnotation.superScopes()).collect(Collectors.toList()));
        }
        log.info("Final Method Scopes for resourceInfo:{}, scopes:{} ", resourceInfo, scopes);
    }
    
    public static List<String> getMethodSuperScopes(Method resourceMethod) {
        log.info("getMethodSuperScopes() - Get Scopes for resourceMethod:{}", resourceMethod);
        List<String> superScopes = null ;
        
        if(resourceMethod==null) {
            return superScopes;
        }
        
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);

        if (methodAnnotation != null) {
            superScopes = new ArrayList<>();
            superScopes.addAll(Stream.of(methodAnnotation.superScopes()).collect(Collectors.toList()));
        }
        
        log.info("getMethodSuperScopes:() - Final Scopes for resourceMethod:{}, superScopes:{} ", resourceMethod, superScopes);
        return superScopes;
    }


    public String requestAccessToken(final String clientId, final List<String> scope) {
        log.info("Request for AccessToken - clientId:{}, scope:{} ", clientId, scope);
        String tokenUrl = getTokenEndpoint();
        Token token = getAccessToken(tokenUrl, clientId, scope);
        log.debug("oAuth AccessToken response - token:{}", token);
        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }

    public Token getAccessToken(final String tokenUrl, final String clientId, final List<String> scopes) {
        log.info("Access Token Request - tokenUrl:{}, clientId:{}, scopes:{}", tokenUrl, clientId, scopes);

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
    
    public List<String> getClientScope(final String clientId) {
        log.info("Get scopes of Client:{} ", clientId);
        List<String> scopes = null;
        
        if(StringUtils.isBlank(clientId)) {
            return scopes;
        }
        // Get Client
        Client client = this.clientService.getClientByInum(clientId);
        if (client == null) {
            return scopes;
        }

        // Prepare scope array
         String[] scopeArray = client.getScopes();
        log.info(" scope to be scopeArray - {} ", Arrays.asList(scopeArray));
        if(scopeArray==null || scopeArray.length<=0) {
            return scopes;
        }
        
        // Assign scope
        scopes = getScopeFromDn(scopeArray);
        log.info(" Scope of clientId:{} is :{} ", clientId, scopes);
        return scopes;
    }
    
    
    public List<String> getScopeFromDn( String[] scopes) {
        List<String> scopeList = null;
        if (scopes != null && scopes.length>0) {
            scopeList = new ArrayList<>();
            for (String dn : scopes) {
                Scope scope = this.scopeService.getScopeByDn(dn);
                if(scope!=null) {
                scopeList.add(scope.getId());
                }
            }
        }
        return scopeList;
    }

    public void assignAllScope(final String clientId) {
        log.info("Client to be assigned all scope - {} ", clientId);

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
        Map<String, Scope> scopeMap = ApiProtectionCache.getAllTypesOfScopes();
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
        log.info("Fetch Auth server specific scope for resourceInfo:{} ", resourceInfo);

        // Get required oauth scopes for the endpoint
        List<String> resourceScopes = getAllScopeList(getRequestedScopes(resourceInfo));
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

        log.info("Applicable exclusiveAuthScopes for resourceInfo:{} are {} ", resourceInfo, exclusiveAuthScopesToReq);
        return exclusiveAuthScopesToReq;
    }

    public List<String> findMissingElements(List<String> list1, List<String> list2) {
        if (list1 == null || list1.isEmpty()) {
            return Collections.emptyList();
        }
        if(list2==null || list2.isEmpty()) {
            return list1;
        }
        return list1.stream().filter(e -> !list2.contains(e)).collect(Collectors.toList());
    }

    public boolean containsAnyElement(List<String> list1, List<String> list2) {
        if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
            return false;
        }
        return list1.stream().anyMatch(list2::contains);
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
            return false;
        }
        return CollectionUtils.isEqualCollection(list1, list2);
    }

    public boolean containsField(List<Field> allFields, String attribute) {
        log.debug("allFields:{},  attribute:{}, allFields.contains(attribute):{} ", allFields, attribute,
                allFields.stream().anyMatch(f -> f.getName().equals(attribute)));

        return allFields.stream().anyMatch(f -> f.getName().equals(attribute));
    }

    public List<Field> getAllFields(Class<?> type) {
        List<Field> allFields = new ArrayList<>();
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

    public List<String> getAllScopeList(Map<ProtectionScopeType, List<String>> scopeMap) {
        List<String> scopeList = new ArrayList<>();
        log.debug("Get all scopeMap:{} ", scopeMap);
        if (scopeMap == null || scopeMap.isEmpty()) {
            return scopeList;
        }

        scopeList = scopeMap.get(ProtectionScopeType.SCOPE);
        log.debug("Get all scopeList:{} ", scopeList);
        return scopeList;

    }

    public Date parseStringToDateObj(String dateString) {
        String datePattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        log.debug("parseStringToDateObj:{} ", dateString);
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            log.error("Error in parsing string to date. Allowed Date Format : {},  Date-String : {} ",
                    datePattern, dateString);
        }
        return date;
    }
    
    public ByteArrayOutputStream getByteArrayOutputStream(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(input ==null) {
            return baos;
        }
        
        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        return baos;
    }
    
    public InputStream getInputStream(ByteArrayOutputStream output) {
        InputStream input = null;
        if (output == null) {
            return input;
        }

        return new ByteArrayInputStream(output.toByteArray());  
    }
    
    public static String readFile(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath();
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> findMissingScopes(Map<ProtectionScopeType, List<String>> scopeMap, List<String> tokenScopes) {
        log.info("Check scopeMap:{}, tokenScopes:{}", scopeMap, tokenScopes);
        List<String> scopeList = new ArrayList<>();
        if (scopeMap == null || scopeMap.isEmpty()) {
            return scopeList;
        }

        // Super scope
        scopeList = scopeMap.get(ProtectionScopeType.SUPER);
        log.debug("SUPER Scopes:{}", scopeList);
        List<String> missingScopes = null;
        boolean containsScope = false;
        if (scopeList != null && !scopeList.isEmpty()) {
            // check if token contains any of the super scopes
            containsScope = containsAnyElement(scopeList, tokenScopes);
            log.debug("Token contains SUPER scopes?:{}", containsScope);

            // Super scope present so no need to check other types of scope
            if (containsScope) {
                return missingScopes;
            }
        }

        // Group scope present so no need to check normal scope presence
        scopeList = scopeMap.get(ProtectionScopeType.GROUP);
        log.debug("GROUP Scopes:{}", scopeList);
        if (scopeList != null && !scopeList.isEmpty()) {
            // check if token contains any of the group scopes
            containsScope = containsAnyElement(scopeList, tokenScopes);
            log.debug("Token contains GROUP scopes?:{}", containsScope);

            // Group scope present so no need to check normal scope
            if (containsScope) {
                return missingScopes;
            }
        }

        // Normal scope
        scopeList = scopeMap.get(ProtectionScopeType.SCOPE);
        log.debug("SCOPE Scopes:{}", scopeList);
        if (scopeList != null && !scopeList.isEmpty()) {
            // check if token contains all the required scopes
            missingScopes = findMissingElements(scopeList, tokenScopes);
            log.debug("SCOPE Missing Scopes:{}", missingScopes);
        }
        return missingScopes;
    }

    public Map<ProtectionScopeType, List<String>> getResourceScopesByType(ResourceInfo resourceInfo) {
        // Get resource scope
        return getRequestedScopes(resourceInfo);
    }

    public List<String> getUserRolePermission(HttpHeaders httpHeaders) {

        List<String> userPermissionList = new ArrayList<>();
        Set<String> userPermissionSet = null;
        
        // Get userInum from header 
        String userInum = getUserInum(httpHeaders);
        log.info("userInum:{}", userInum);
        if(StringUtils.isBlank(userInum)) {
            return userPermissionList; 
        }

        // Get User details based on userInum
        User user = getUserByInum(userInum);
        log.info("userInum:{}, user:{}", userInum, user);

        //Get user roles from DB
        List<String> userRoleList = getUserRole(user);
        log.info("userInum:{}, userRoleList:{}", userInum, userRoleList);
        if (userRoleList == null || userRoleList.isEmpty()) {
            return userPermissionList;
        }
        log.info("userInum:{}, user:{}, userRoleList:{}", userInum, user, userRoleList);

        //Fetch distinct permissions associated with role
        Set<String> safeSet = new HashSet<>(userRoleList);
        userPermissionSet = getUserPermission(safeSet);
        if(userPermissionSet == null || userPermissionSet.isEmpty()) {
            return userPermissionList;
        }
        userPermissionList = new ArrayList<>(userPermissionSet);        
        log.info("userInum:{},userRole:{}, userPermissionSet:{}, userPermissionList:{}", userInum, userRoleList, userPermissionSet, userPermissionList);

        return userPermissionList;
    }

    public Set<String> getUserPermission(Set<String> userRoleSet) {
        log.info("userRoleSet:{}", userRoleSet);
        Set<String> userPermissionSet = new HashSet<>();
        if (userRoleSet == null || userRoleSet.isEmpty()) {
            return userPermissionSet;
        }

        for (String userRole : userRoleSet) {
            RolePermissionMapping rolePermissionMapping = getPermissionsMappingByRole(userRole);
            if (rolePermissionMapping == null) {
                continue;
            }
            userPermissionSet.addAll(rolePermissionMapping.getPermissions());
        }

        log.info("userPermissionSet:{}", userPermissionSet);
        return userPermissionSet;
    }

    public List<String> getUserRole(User user) {
        log.info("getUserRole() - user:{}", user);
        List<String> userRoleList = null;

        if (user == null) {
            return userRoleList;
        }

        List<CustomObjectAttribute> customAttributes = user.getCustomAttributes();
        if (customAttributes == null || customAttributes.isEmpty()) {
            return userRoleList;
        }

        userRoleList = getAttributeValueList(customAttributes, getUserRoleAttributeName());
        log.info(" user.getUserId():{}, getUserRoleAttributeName():{}, userRoleList:{}", user.getUserId(), getUserRoleAttributeName(), userRoleList);
        if (userRoleList.isEmpty()) {
            return userRoleList;
        }

        log.info(" Returning user.getUserId():{}, userRoleList:{}", user.getUserId(), userRoleList);
        return userRoleList;
    }

    public static CustomObjectAttribute getAttribute(List<CustomObjectAttribute> customAttributes,
            String attributeName) {
        if (customAttributes == null || customAttributes.isEmpty() || StringUtils.isBlank(attributeName)) {
            return null;
        }
        return customAttributes.stream().filter(ca -> ca.getName().equals(attributeName)).findFirst().orElse(null);
    }

    public static List<String> getAttributeValueList(List<CustomObjectAttribute> customAttributes,
            String attributeName) {
        List<String> attributeValueList = new ArrayList<>();
        List<Object> list = Optional.ofNullable(getAttribute(customAttributes, attributeName))
                .map(CustomObjectAttribute::getValues).orElse(Collections.emptyList()).stream().filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (list == null || list.isEmpty()) {
            return attributeValueList;
        }

        for (Object obj : list) {
            if (obj.getClass().equals(String.class)) {
                attributeValueList.add(String.class.cast(obj));
            }
        }

        return attributeValueList;
    }

    public String getUserInum(HttpHeaders httpHeaders) {
        String userInum = null;
        if (httpHeaders == null) {
            return userInum;
        }
        userInum = httpHeaders.getHeaderString("User-inum");
        log.info(" Logged in userInum:{}", userInum);
        return userInum;
    }

    public User getUserByInum(String inum) {
        User user = null;
        if (StringUtils.isBlank(inum)) {
            return user;
        }
        return rolePermissionMappingService.getUserByInum(inum);
    }

    public RolePermissionMapping getPermissionsMappingByRole(String role) {
        return rolePermissionMappingService.getPermissionsMappingByRole(role);
    }

    public IntrospectionResponse getIntrospectionResponse(String token) {
        try {
            return openIdService.getIntrospectionResponse(token,
                    token.substring("Bearer".length()).trim(), this.getAuthIssuerUrl());
        } catch (JsonProcessingException ex) {
            log.error("AuthUtil::getIntrospectionResponse() - Error while token Introspection token:{}, exception:{} ", token, ex);
            throw new WebApplicationException("AuthUtil::getIntrospectionResponse - Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    public String getJsonNodeKeyValue(JsonNode jsonNode, String key) {
        String keyValue = null;
        if (jsonNode == null || StringUtils.isBlank(key)) {
            return keyValue;
        }
        JsonNode keyNode = jsonNode.get(key);
        if (keyNode == null || keyNode.isNull()) {
            return keyValue;
        }
        return keyNode.asText();
    }
    
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw); // Redirects the trace output into the StringWriter
        return sw.toString();
    }
    
    public static List<String> getListFromRawObject(Object rawObject){
        return Optional.ofNullable(rawObject)
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .orElse(Collections.emptyList())
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();         
    }
    
    public static Optional<String> getStringFromObject(Object raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof String s) {
            return Optional.of(s);
        }
        throw new IllegalStateException(
                "Expected String, found " + raw.getClass().getName());
    }

    public static List<String> getListFromObject(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }

        if (!(raw instanceof List<?> rawList)) {
            throw new IllegalStateException(
                    "Expected List, found " + raw.getClass().getName());
        }

        // Verify every element is actually a String before the cast —
        // type erasure can't check this for you.
        for (Object element : rawList) {
            if (!(element instanceof String)) {
                throw new IllegalStateException(
                        "Expected List<String>, found element of type "
                                + (element == null ? "null" : element.getClass().getName()));
            }
        }

        @SuppressWarnings("unchecked") // safe: every element verified above
        List<String> result = (List<String>) rawList;
        return Collections.unmodifiableList(result);
    }

}
