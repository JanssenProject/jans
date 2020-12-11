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
import java.util.ArrayList;
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
    UmaResourceProtectionCache resourceProtectionCache;

    @Inject
    UmaService umaService;

    @Inject
    EncryptionService encryptionService;

    @PostConstruct
    public void init() throws Exception {
        // Create clients if needed
        createClientIfNeeded();

        // If test mode then create create token with scopes
        System.out.println("\n\n isTestMode() = " + isTestMode() + "\n\n");
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
        System.out.println("AuthUtil:::isTestMode() - configurationFactory.getAppExecutionMode() = "
                + configurationFactory.getAppExecutionMode());
        return configurationFactory.getAppExecutionMode() != null
                && "TEST".equalsIgnoreCase(configurationFactory.getAppExecutionMode());
    }

    public String getTokenUrl() {
        System.out.println("AuthUtil:::getTokenUrl() - this.configurationService.find().getTokenEndpoint() = "
                + this.configurationService.find().getTokenEndpoint() + " \n\n");
        return this.configurationService.find().getTokenEndpoint();
    }

    public Client getClient(String clientId) {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::getClient() - clientId = " + clientId + "\n\n\n\n");

        // Get client
        Client client = clientService.getClientByInum(clientId);
        System.out.println("\n\n\n AuthUtil::getClient() - client = " + client + " , client.getClientId() = "
                + client.getClientId() + " , client.getClientSecret() = " + client.getClientSecret() + "\n\n\n\n");

        return client;
    }

    public String getClientPassword(String clientId) {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::getClient() - clientId = " + clientId + "\n\n\n\n");
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

    public List<String> getRequestedScopes(String path) {
        UmaResource resource = resourceProtectionCache.getUmaResource(path);
        log.debug(" resource = " + resource);
        return resource.getScopes();
    }

    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
        List<String> scopes = new ArrayList<String>();
        if (typeAnnotation != null) {
            scopes.addAll(Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
        }
        return scopes;
    }

    public Token requestPat(final String tokenUrl, final String clientId, final List<String> scopes) throws Exception {
        return request(tokenUrl, clientId, this.getClientDecryptPassword(clientId), scopes);
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
                scope = scope + " " + s;
            }
        }

        TokenResponse tokenResponse = AuthClientFactory.patRequest(tokenUrl, clientId, clientSecret, scope);

        if (tokenResponse != null) {

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
        System.out.println(
                "\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::requestRpt() - clientId = " + clientId + " ,  resourceId = "
                        + resourceId + " ,  scopes = " + scopes + " , patToken  = " + patToken + " \n\n\n\n");

        // Get client
        Client client = getClient(clientId);
        System.out.println("\n\n\n AuthUtil::requestPat() - client = " + client + " , client.getClientId() = "
                + client.getClientId() + " , client.getClientSecret() = " + client.getClientSecret() + "\n\n\n\n");

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
        System.out.println(
                "\n\n\n AuthUtil::registerTestClientRptTicket() - UmaPermission = " + umaPermission + "\n\n\n\n");
        PermissionTicket permissionTicket = umaService.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(umaPermission));

        System.out.println("\n\n\n AuthUtil::registerTestClientRptTicket() FINAL - permissionTicket = "
                + permissionTicket + "\n\n\n\n");
        if (permissionTicket == null) {
            return null;
        }

        // Register RPT token
        TokenResponse tokenResponse = null;
        try {
            // rptStatusResponse =
            // this.getUmaRptIntrospectionService().requestRptStatus(authorization,rptToken,"");
            tokenResponse = AuthClientFactory.requestRpt(this.getTokenUrl(), client.getClientId(),
                    this.decryptPassword(client.getClientSecret()), scopes, permissionTicket.getTicket(),
                    GrantType.OXAUTH_UMA_TICKET, AuthenticationMethod.CLIENT_SECRET_BASIC);

            System.out.println("\n\n AuthUtil::getStatusResponse() - tokenResponse  = " + tokenResponse);
            System.out.println(
                    "\n\n AuthUtil::getStatusResponse() - tokenResponse.toString()  = " + tokenResponse.toString());
            System.out.println("\n\n AuthUtil::getStatusResponse() - okenResponse.getAccessToken()  = "
                    + tokenResponse.getAccessToken());
        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }

        return tokenResponse;
    }

    public String testPrep(ResourceInfo resourceInfo, String method, String path) throws Exception {
        Token token = null;
        if (ApiConstants.PROTECTION_TYPE_OAUTH2.equals(this.getApiProtectionType())) {
            token = requestPat(getTokenUrl(), this.getClientId(), getRequestedScopes(resourceInfo));
        } else {
            token = registerTestClientRptTicket(resourceInfo, method, path);
        }
        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }

    public Token registerTestClientRptTicket(ResourceInfo resourceInfo, String method, String path) throws Exception {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - resourceInfo = "
                + resourceInfo + "\n\n\n\n");

        List<String> scopes = this.getRequestedScopes(resourceInfo);
        System.out.println(
                "\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - scopes = " + scopes + "\n\n");

        // Get Pat
        Token patToken = requestPat(this.getTokenUrl(), this.getClientId(), scopes);
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ AuthUtil::registerTestClientRptTicket() - patToken = " + patToken
                + "\n\n\n\n");

        UmaResource umaResource = this.getUmaResource(resourceInfo, method, path);
        if (patToken != null && umaResource != null) {

            // Register RPT token
            TokenResponse tokenResponse = this.requestRpt(this.getClientId(), umaResource.getId(), scopes, patToken);
            System.out.println("\n\n\n AuthUtil::registerTestClientRptTicket() FINAL - tokenResponse = " + tokenResponse
                    + "\n\n\n\n");

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
        System.out.println(" \n\n AuthUtil::createClientIfNeeded() - clientList = " + clientList + "\n\n");
        List<Client> clients = clientList.getClients();

        System.out.println(" \n\n AuthUtil::createClientIfNeeded() - clients = " + clients + "\n\n");

        Preconditions.checkNotNull(clients, "Config Api Client list cannot be null !!!");

        // Create client
        for (Client clt : clients) {
            System.out.println(" \n\n AuthUtil::createClientIfNeeded() - clt = " + clt + "\n\n");
            // Check if exists
            Client client = null;

            try {
                client = this.clientService.getClientByInum(clt.getClientId());
                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Verify client = " + client + "\n\n");

            } catch (Exception ex) {
                log.error("Error while searching client " + ex);
            }

            System.out.println(
                    "\n\n @@@@@@@@@@@@@@@@@@@@@@@ AuthUtil::createClientIfNeeded() - Before encryption clt.getClientSecret()  = "
                            + clt.getClientSecret());

            if (client == null) {
                // Create client
                clt.setDn(clientService.getDnForClient(clt.getClientId()));
                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Create clt = " + clt + "\n\n");
                this.clientService.addClient(clt);
            } else {
                clt.setDn(clientService.getDnForClient(clt.getClientId()));

                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Update clt = " + clt + "\n\n");
                this.clientService.updateClient(clt);
            }

            client = this.clientService.getClientByInum(clt.getClientId());
            System.out.println(" \n\n @@@@@@@@@@@@@@@@@@@@@@@ AuthUtil::createClientIfNeeded() - Final client = "
                    + client + "\n\n");

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

}
