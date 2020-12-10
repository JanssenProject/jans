package io.jans.configapi.auth.util;

import com.google.common.base.Preconditions;

import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.auth.client.UmaClient;
import io.jans.configapi.auth.service.UmaService;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.ClientService;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import io.jans.configapi.util.Jackson;
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
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class AuthUtil {
    
    @Inject
    Logger log;
    
    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject
    ClientService clientService;
    
    @Inject
    UmaService umaService;
    
    @Inject
    EncryptionService encryptionService;

    public AuthUtil() {}
    
    
    @PostConstruct
    public void init() throws Exception {
        // Create clients if needed
        createClientIfNeeded();

        // If test mode then create create token with scopes
        System.out.println("\n\n isTestMode() = "+isTestMode()+"\n\n");
    }

    private void createClientIfNeeded() throws Exception { 
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("api-client.json");
        
        ClientList clientList = Jackson.createJsonMapper().readValue(inputStream, ClientList.class);
        System.out.println(
                " \n\n AuthUtil::createClientIfNeeded() - clientList = " + clientList + "\n\n");
        List<Client> clients = clientList.getClients();

        System.out.println(" \n\n AuthUtil::createClientIfNeeded() - clients = "
                + clients + "\n\n");

        Preconditions.checkNotNull(clients, "Config Api Client list cannot be null !!!");
        
        //Create client
        for (Client clt : clients) {
            System.out.println(" \n\n AuthUtil::createClientIfNeeded() - clt = "
                    + clt + "\n\n");
            // Check if exists
            Client client = null;

            try {
                client = this.clientService.getClientByInum(clt.getClientId());
                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Verify client = "
                        + client + "\n\n");

            } catch (Exception ex) {
                log.error("Error while searching client "+ex);
            }
            
            System.out.println("\n\n @@@@@@@@@@@@@@@@@@@@@@@ AuthUtil::createClientIfNeeded() - Before encryption clt.getClientSecret()  = "+clt.getClientSecret() );
           
            if (client == null) {
                // Create client           
                clt.setDn(clientService.getDnForClient(clt.getClientId())); 
                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Create clt = "
                        + clt + "\n\n");
                this.clientService.addClient(clt);
            } else {
                clt.setDn(clientService.getDnForClient(clt.getClientId()));
               
                System.out.println(" \n\n AuthUtil::createClientIfNeeded() - Update clt = "
                        + clt + "\n\n");
                this.clientService.updateClient(clt);
            }

            client = this.clientService.getClientByInum(clt.getClientId());
            System.out.println(
                    " \n\n @@@@@@@@@@@@@@@@@@@@@@@ AuthUtil::createClientIfNeeded() - Final client = " + client + "\n\n");


        }

    }
    
    private boolean isTestMode() {
        System.out.println("AuthUtil:::isTestMode() - configurationFactory.getAppExecutionMode() = "+configurationFactory.getAppExecutionMode());
         return configurationFactory.getAppExecutionMode()!=null && "TEST".equalsIgnoreCase(configurationFactory.getAppExecutionMode());
    }
    
    
    public String createcreateTestClientPermission(String resourceId, List<String> scopes, String url, String clientId  ) throws Exception {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ UmaService::createTestClientPermission() - resourceId = "+resourceId+" ,  scopes = "+scopes+" , url = "+url+" , clientId = "+clientId+"\n\n\n\n");
        
        //Get client
        Client client = clientService.getClientByInum(clientId);
        System.out.println("\n\n\n UmaService::createTestClientPermission() - client = "+client+" , client.getClientId() = "+client.getClientId()+" , client.getClientSecret() = "+client.getClientSecret()+"\n\n\n\n");
        
        String[] scopeArrray = null;
        if(scopes!=null)
            scopeArrray = (String[])scopes.toArray();
        
        Token patToken = UmaClient.requestPat(url,client.getClientId(), client.getClientSecret(), scopeArrray);

        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ UmaService::createTestClientPermission() - patToken = "+patToken+" , patToken.getAccessToken() = "+patToken.getAccessToken()+"\n\n\n\n");        
        UmaPermission permission = new UmaPermission();
        permission.setResourceId(resourceId);
        permission.setScopes(scopes);
        System.out.println("\n\n\n UmaService::createTestClientPermission() - permission = "+permission+"\n\n\n\n");


        PermissionTicket ticket = umaService.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(permission));

        System.out.println("\n\n\n UmaService::createTestClientPermission() FINAL - ticket = " + ticket + "\n\n\n\n");
        if (ticket == null) {
            return null;
        }

        // Register RPT token 
        TokenResponse tokenResponse = null;
        try {
            // rptStatusResponse = this.getUmaRptIntrospectionService().requestRptStatus(authorization,rptToken,"");
            tokenResponse = UmaClient.requestRpt("https://pujavs.jans.server/jans-auth/restv1/token", "1802.9dcd98ad-fe2c-4fd9-b717-d9436d9f2009","test1234",scopes,ticket.getTicket());

            System.out.println("\n\n UmaService::getStatusResponse() - tokenResponse  = " + tokenResponse);
            System.out.println("\n\n UmaService::getStatusResponse() - tokenResponse.toString()  = " + tokenResponse.toString());
            System.out.println("\n\n UmaService::getStatusResponse() - okenResponse.getAccessToken()  = " + tokenResponse.getAccessToken());
        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }
        return ticket.getTicket();
    }
    

}
