package io.jans.configapi.util;

import com.google.common.base.Preconditions;

import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.register.ApplicationType;
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
import io.jans.configapi.auth.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ClientService;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.Jackson;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.apache.commons.lang.RandomStringUtils;
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
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class ApiTestMode {

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    ClientService clientService;
    
    private static String testClientId;
    

    public Client init() {
            return createTestClient();
    }

    public String getApiProtectionType() {
        return this.configurationFactory.getApiProtectionType();
    }

    public String createTestToken(String clientId, ResourceInfo resourceInfo, String method, String path) throws Exception {
        log.trace(" Creating Test Token, clientId: {}, resourceInfo: {}, method: {}, path: {} ", clientId,resourceInfo, method, path);
        Token token = null;

        // Get all scopes
        List<String> scopes = this.authUtil.getRequestedScopes(resourceInfo);
        if (ApiConstants.PROTECTION_TYPE_OAUTH2.equals(this.getApiProtectionType())) {
            token = this.authUtil.requestAccessToken(this.authUtil.getTokenUrl(), clientId, scopes);
        } else {
            token = registerRptTicket(clientId,resourceInfo, method, path, scopes);
        }
        log.trace("Generated token: {} ", token);

        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }

    private Token registerRptTicket(String clientId, ResourceInfo resourceInfo, String method, String path, List<String> scopes)
            throws Exception {
        log.trace(" Register Rpt Ticket, clientId:{}, resourceInfo: {}, method: {}, path: {}, scopes: {}", clientId, resourceInfo, method,
                path, scopes);

        // Get Pat
        Token patToken = this.authUtil.requestPat(this.authUtil.getTokenUrl(), clientId, ScopeType.UMA,
                scopes);
        log.trace(" Rpt patToken: {}", patToken);

        UmaResource umaResource = this.authUtil.getUmaResource(resourceInfo, method, path);
        if (patToken != null && umaResource != null) {

            // Register RPT token
            TokenResponse tokenResponse = this.authUtil.requestRpt(clientId, umaResource.getId(), scopes,
                    patToken);
            log.debug("tokenResponse = " + tokenResponse + "\n");

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

    private Client createTestClient()  {
        // Create test client
        //String clientPassword = "test1234";
        String clientPassword = RandomStringUtils.randomAlphanumeric(8);
        Client client = new Client();
        client.setClientSecret(authUtil.encryptPassword(clientPassword));
        client.setApplicationType(ApplicationType.NATIVE);
        client.setClientName("Test_Client_1");
        client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN,
                GrantType.CLIENT_CREDENTIALS, GrantType.OXAUTH_UMA_TICKET });
        client.setScopes(this.getAllScopeArray());
        client.setResponseTypes(new ResponseType[] { ResponseType.CODE });
        client.setSubjectType(SubjectType.PAIRWISE);
        client.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC.toString());

        String inum = clientService.generateInumForNewClient();
        client.setClientId(inum);
        client.setDn(clientService.getDnForClient(inum));
        clientService.addClient(client);
       
        Client result = clientService.getClientByInum(inum);
        testClientId = result.getClientId();
        return result;
    }

    public void deleteTestClient(String clientId) {
        System.out.println("\n\n ApiTestMode:::deleteTestClient() - clientId = "+clientId+"\n\n");
        Client client = this.clientService.getClientByInum(clientId);
        System.out.println("Client to delete " + client.getClientId()+"\n\n");
        if (client != null) {
            this.clientService.removeClient(client);
        }
    }

    public String[] getAllScopeArray() {
        List<String> scopes = this.authUtil.getScopeWithDn(this.authUtil.getAllScopes());
        return this.authUtil.getAllScopesArray(scopes);
    }

    private Date getCreationDate(RsResource rsResource) {
        final Calendar calendar = Calendar.getInstance();
        Date iat = calendar.getTime();

        if (rsResource.getIat() != null && rsResource.getIat() > 0) {
            iat = new Date(rsResource.getIat() * 1000L);
        }
        return iat;
    }

    private Date getExpDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date()); // Now use today date.
        calendar.add(Calendar.DATE, 1); // Adds 1 days
        Date exp = calendar.getTime();
        return exp;
    }

}
