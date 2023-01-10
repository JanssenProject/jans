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
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.model.configuration.DataFormatConversionConf;
import io.jans.configapi.security.api.ApiProtectionCache;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;

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
    
    public AuditLogConf getAuditLogConf() {
        return this.configurationFactory.getApiAppConfiguration().getAuditLogConf();
    }
    
    public DataFormatConversionConf getDataFormatConversionConf() {
        return this.configurationFactory.getApiAppConfiguration().getDataFormatConversionConf();
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
        if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
            return Collections.emptyList();
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

    public RevokeSessionResponse revokeSession(final String url, final String token, final String userId) {
        log.debug("Revoke session Request - url:{}, token:{}, userId:{}", url, token, userId);

        RevokeSessionResponse revokeSessionResponse = AuthClientFactory.revokeSession(url, token, userId);
        log.debug("revokeSessionResponse:{}", revokeSessionResponse);
        if (revokeSessionResponse != null) {
            log.debug("revokeSessionResponse.getEntity():{}, revokeSessionResponse.getStatus():{} ",
                    revokeSessionResponse.getEntity(), revokeSessionResponse.getStatus());
        }
        return revokeSessionResponse;
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
        String DATE_PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN_YYYY_MM_DD);
        log.debug("parseStringToDateObj:{} ", dateString);
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            log.error("Error in parsing string to date. Allowed Date Format : {},  Date-String : {} ", DATE_PATTERN_YYYY_MM_DD, dateString);
        }
        return date;
    }

}
