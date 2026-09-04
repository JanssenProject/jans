package io.jans.configapi.util;

import com.unboundid.ldap.sdk.DN;
import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.common.User;
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
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Inject
    ConfService confService;
    
    @Inject 
    RolePermissionMappingService rolePermissionMappingService;

    public String getOpenIdConfigurationEndpoint() {
        return this.configurationService.find().getOpenIdConfigurationEndpoint();
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
    
    public boolean isUserRolePermissionValidationEnabled() {
        return this.configurationFactory.getApiAppConfiguration().isUserRolePermissionValidationEnabled();
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

    public List<String> validateUserRolePermission(ResourceInfo resourceInfo, HttpHeaders httpHeaders) {
        log.error("\n\n\n validateUserRolePermission - param resourceInfo:{}, httpHeaders:{}", resourceInfo,
                httpHeaders);

        List<String> missingScopes = null;
        Set<String> userCurrentScopes = this.getUserRolePermission(httpHeaders);
        log.info("userCurrentScopes:{}", userCurrentScopes);

        // find missing scopes
        Map<ProtectionScopeType, List<String>> resourceScopesByType = getResourceScopesByType(resourceInfo);
        log.error("resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return missingScopes;
        }

        List<String> resourceScopes = getAllScopeList(resourceScopesByType);
        log.debug("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

        List<String> safeList = new ArrayList<>(userCurrentScopes);
        missingScopes = findMissingScopes(resourceScopesByType, safeList);
        log.info("missingScopes:{}", missingScopes);

        return missingScopes;
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

    public Set<String> getUserRolePermission(HttpHeaders httpHeaders) {

        Set<String> userPermissionSet = null;
        // Get user
        String userInum = getUserInum(httpHeaders);
        log.error("userInum:{}", userInum);
        // Get User details
        User user = getUserByInum(userInum);
        log.error("userInum:{}, user:{}", userInum, user);

        List<String> userRoleList = getUserRole(user);
        log.error("userInum:{}, userRoleList:{}", userInum, userRoleList);
        if (userRoleList == null || userRoleList.isEmpty()) {
            return userPermissionSet;
        }
        log.error("userInum:{}, user:{}, userRoleList:{}", userInum, user, userRoleList);

        Set<String> safeSet = new HashSet<>(userRoleList);
        userPermissionSet = getUserPermission(safeSet);

        log.error("userInum:{},userRole:{}, userPermissionSet:{}", userInum, userRoleList, userPermissionSet);

        return userPermissionSet;
    }

    public Set<String> getUserPermission(Set<String> userRoleSet) {
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
        return userPermissionSet;
    }

    public List<String> getUserRole(User user) {
        List<String> userRoleList = null;
        List<CustomObjectAttribute> customAttributes = user.getCustomAttributes();
        if (customAttributes == null || customAttributes.isEmpty()) {
            return userRoleList;
        }

        List<String> attributeValueList = getAttributeValueList(customAttributes, "jansAdminUIRole");
        log.error(" user:{}, jansAdminUIRole-attributeValueList:{}", user, attributeValueList);
        if (attributeValueList == null || attributeValueList.isEmpty()) {
            return userRoleList;
        }

        log.error(" user:{}, userRoleList:{}", user, userRoleList);
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

        String client = httpHeaders.getHeaderString("jans-client");
        userInum = httpHeaders.getHeaderString("User-inum");
        log.error("client:{} - userInum:{}", client, userInum);
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
        log.error("role:{}", role);
        return rolePermissionMappingService.getPermissionsMappingByRole(role);
    }
}
