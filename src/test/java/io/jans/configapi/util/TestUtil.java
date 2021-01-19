package io.jans.configapi.util;

import com.google.common.base.Preconditions;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.ca.rs.protect.RsResource;
import io.jans.configapi.auth.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.ClientService;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TestUtil {

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    ClientService clientService;

    private static String testClientId = "1800.9test";

    public String getTestClientId() {
        return testClientId;
    }

    public Client init() {
        return createTestClient();
    }

    public static String getApiProtectionType() {
        return ConfigurationFactory.getApiProtectionType();
    }

    public String createTestToken(String method , String path)
            throws Exception {
        log.trace("\n\n\n *******************  TestUtil:::createTestToken() - Entry - method = "+method+" ,path = "+path+" ******************* ");
        //log.trace(" Creating Test Token, path: {}, method: {} ", path, method);
        Preconditions.checkNotNull(method, "Method cannot be null !!!");
        Preconditions.checkNotNull(path, "Path cannot be null !!!");
        
                
        Token token = null;
        String clientId = this.getTestClientId();
        Client client = this.clientService.getClientByInum(clientId);
        log.trace("\n\n\n TestUtil:::createTestToken() - client = "+Arrays.toString(client.getScopes())+"\n\n\n");
        
        // Get all scopes
        List<String> scopes = this.authUtil.getRequestedScopes(method,path);
        log.trace("\n\n\n TestUtil:::createTestToken() - scopes = "+scopes+"\n\n\n");
        token = this.authUtil.requestAccessToken(this.authUtil.getTokenUrl(), clientId, scopes);
        
        log.trace("Generated token: {} ", token);

        if (token != null) {
            return token.getAccessToken();
        }
        return null;
    }
    
    private Client createTestClient() {
        
        String inum = testClientId;
        Client client = clientService.getClientByInum(inum);
        log.trace("\n\n\n\n :::createTestClient() - client_1 = "+client+"\n\n\n");
        if(client==null) {            
            String clientPassword = RandomStringUtils.randomAlphanumeric(8);
            client = new Client();
            client.setClientSecret(authUtil.encryptPassword(clientPassword));
            client.setApplicationType(ApplicationType.NATIVE);
            client.setClientName("Test_Client_1");
            client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN,
                    GrantType.CLIENT_CREDENTIALS, GrantType.OXAUTH_UMA_TICKET });
            client.setScopes(this.getAllScopeArray());
            client.setResponseTypes(new ResponseType[] { ResponseType.CODE });
            client.setSubjectType(SubjectType.PAIRWISE);
            client.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
                          
            client.setClientId(inum);
            client.setDn(clientService.getDnForClient(inum));
            clientService.addClient(client);
        }
        
        client = clientService.getClientByInum(inum);
        log.trace("\n\n\n\n :::createTestClient() - client_2 = "+client+"\n\n\n");
        testClientId = client.getClientId();
        return client;
    }

    public void deleteTestClient() {
        String clientId = this.getTestClientId();
        log.trace("\n\n TestUtil:::deleteTestClient() - clientId = " + clientId + "\n\n");
        Client client = clientService.getClientByInum(clientId);
        log.trace("Client to delete " + client.getClientId() + "\n\n");
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
