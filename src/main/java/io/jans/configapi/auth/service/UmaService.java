/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

import io.jans.as.client.TokenResponse;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.auth.client.UmaClient;
import io.jans.configapi.service.ConfigurationService;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.orm.util.StringHelper;
import io.jans.util.init.Initializable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Named("umaService")
public class UmaService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    UmaMetadata umaMetadata;
    UmaMetadataService umaMetadataService;
    UmaPermissionService umaPermissionService;
    UmaRptIntrospectionService umaRptIntrospectionService;

    @PostConstruct
    public void init() {
        this.umaMetadataService = AuthClientFactory
                .getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
        this.umaMetadata = umaMetadataService.getMetadata();
        this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
        this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
    }

    public UmaMetadataService getUmaMetadataService() {
        return this.umaMetadataService;
    }

    @Produces
    @ApplicationScoped
    @Named("umaMetadata")
    public UmaMetadata getUmaMetadata() {
        return this.umaMetadata;
    }

    public UmaPermissionService getUmaPermissionService() {
        return this.umaPermissionService;
    }

    public UmaRptIntrospectionService getUmaRptIntrospectionService() {
        return this.umaRptIntrospectionService;
    }

    public void validateRptToken(Token patToken, String authorization, String resourceId, List<String> scopeIds) {
        log.trace("Validating RPT, patToken:{}, authorization:{}, resourceId: {}, scopeIds: {} ", patToken, authorization, resourceId, scopeIds);
        System.out.println("\n\n Validating RPT, patToken ="+patToken+", patToken.getAccessToken() = "+patToken.getAccessToken()+" ,authorization = "+authorization+" ,resourceId = "+ resourceId+" ,scopeIds = "+ scopeIds);
        System.out.println("\n\n Validating RPT, patToken ="+patToken.toString());
        System.out.println("\n\n Validating RPT, patToken.getAccessToken() ="+patToken.getAccessToken()+" , patToken.getAuthorizationCode() = "+patToken.getAuthorizationCode()+" , patToken.getIdToken() ="+patToken.getIdToken()+" , patToken.getRefreshToken() ="+patToken.getRefreshToken()+" , patToken.getScope() ="+patToken.getScope()+" , patToken.getExpiresIn() ="+patToken.getExpiresIn()+"\n\n\n");
        
       if (patToken == null) {
            log.info("Token is blank"); 
            Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
            throw new WebApplicationException("Token is blank.", registerPermissionsResponse);
        }

        // for tetsing
        try {
            testClientPermission(resourceId, scopeIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // for testing

        if (StringHelper.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
            String rptToken = authorization.substring(7);
            System.out.println("\n\n UmaService::validateRptToken() - rptToken  = " + rptToken);

            RptIntrospectionResponse rptStatusResponse = getStatusResponse(patToken, rptToken);
            log.debug("RPT status response: {} ", rptStatusResponse);
            System.out.println("RPT status response: {} "+ rptStatusResponse);
            
            if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
                log.warn("Status response for RPT token: '{}' is invalid, will do a retry", rptToken);
            } else {
                boolean rptHasPermissions = isRptHasPermissions(rptStatusResponse);

                if (rptHasPermissions) {
                    
                    // Verify exact resource
                    boolean hasResourcePermission = this.hasResourcePermission(rptStatusResponse, resourceId);
                    if (!hasResourcePermission) {
                        log.error("Status response for RPT token: '{}', Resource Id '{}', not contains right resource permissions", rptToken,resourceId);
                    }
                    
                    // Collect all scopes
                    List<String> returnScopeIds = new LinkedList<String>();
                    for (UmaPermission umaPermission : rptStatusResponse.getPermissions()) {
                        if (umaPermission.getScopes() != null) {
                            returnScopeIds.addAll(umaPermission.getScopes());
                        }
                    }

                    if (returnScopeIds.containsAll(scopeIds)) {
                        return;
                    }

                    log.error("Status response for RPT token: '{}' not contains right permissions", rptToken);
                }
            }
        }

        Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
        throw new WebApplicationException("UMA authentication failed.", registerPermissionsResponse);

    }

    private boolean isRptHasPermissions(RptIntrospectionResponse umaRptStatusResponse) {
        return !((umaRptStatusResponse.getPermissions() == null) || umaRptStatusResponse.getPermissions().isEmpty());
    }
    
    private boolean hasResourcePermission(RptIntrospectionResponse umaRptStatusResponse, String resourceId) {
        return umaRptStatusResponse.getPermissions().stream()
        .anyMatch(p -> p.getResourceId().equalsIgnoreCase(resourceId));
    }

    private RptIntrospectionResponse getStatusResponse(Token patToken, String rptToken) {
        String authorization = "Bearer " + patToken.getAccessToken();
        System.out.println("\n\n UmaService::getStatusResponse() - authorization  = " + authorization+" , rptToken = "+rptToken+"\n\n");

        // Determine RPT token to status
        RptIntrospectionResponse rptStatusResponse = null;
        try {
            // rptStatusResponse = this.getUmaRptIntrospectionService().requestRptStatus(authorization,rptToken,"");
            rptStatusResponse = UmaClient.getRptStatus(this.umaMetadata, authorization, rptToken);
            System.out.println("\n\n UmaService::getStatusResponse() - rptStatusResponse  = " + rptStatusResponse);
        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }

        // Validate RPT status response
        if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
            return null;
        }

        return rptStatusResponse;
    }

    private Response prepareRegisterPermissionsResponse(Token patToken, String resourceId, List<String> scopes) {
        String ticket = registerResourcePermission(patToken, resourceId, scopes);
        Response response = null;
        if (StringHelper.isEmpty(ticket)) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            return response;
        }
        log.debug("Construct response: HTTP 401 (Unauthorized), ticket: '{}'", ticket);
        System.out.println("Construct response: HTTP 401 (Unauthorized), ticket: '{}'"+ ticket);
        try {
            String authHeaderValue = String.format(
                    "UMA realm=\"Authorization required\", host_id=%s, as_uri=%s, ticket=%s",
                    getHost(this.umaMetadata.getIssuer()), configurationService.find().getUmaConfigurationEndpoint(),
                    ticket);
            response = Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", authHeaderValue)
                    .build();
        } catch (MalformedURLException ex) {
            log.error("Failed to determine host by URI", ex);
        }

        return response;
    }

    public String registerResourcePermission(Token patToken, String resourceId, List<String> scopes) {
        UmaPermission permission = new UmaPermission();
        permission.setResourceId(resourceId);
        permission.setScopes(scopes);
        System.out.println("\n\n\n UmaService::registerResourcePermission() FINAL - this.umaPermissionService = "
                + this.umaPermissionService + " , patToken = " + patToken +" , resourceId = "+resourceId+" ,  scopes = "+scopes+"\n\n\n\n");
        
        System.out.println("\n\n\n UmaService::registerResourcePermission() FINAL -patToken.getAccessToken() = " + patToken.getAccessToken() + "\n\n\n\n");
        PermissionTicket ticket = this.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(permission));
        
        System.out.println("\n\n\n UmaService::registerResourcePermission() FINAL - ticket = " + ticket + "\n\n\n\n");
        if (ticket == null) {
            return null;
        }
        return ticket.getTicket();
    }
    
    
    public String testClientPermission(String resourceId, List<String> scopes) throws Exception {
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ UmaService::testClientPermission() - resourceId = "+resourceId+" ,  scopes = "+scopes+"\n\n\n\n");
        Token patToken = UmaClient.requestPat(umaMetadata.getTokenEndpoint(),"1802.9dcd98ad-fe2c-4fd9-b717-d9436d9f2009", "test1234", null);
        
        System.out.println("\n\n\n $$$$$$$$$$$$$$$$$ UmaService::testClientPermission() - patToken = "+patToken+" , patToken.getAccessToken() = "+patToken.getAccessToken()+"\n\n\n\n");        
        UmaPermission permission = new UmaPermission();
        permission.setResourceId(resourceId);
        permission.setScopes(scopes); // UmaService::testClientPermission() - permission = UmaPermission{resourceId='a2a236af-6831-463a-9f6c-cd8814318836', scopes=[64e5fe41-e3f9-4544-b6cf-2cf6dff70aaa], expiresAt=null}
                //javax.ws.rs.NotAcceptableException: HTTP 406 Not Acceptable
        //permission.setScopes(Arrays.asList("https://jans.io/oauth/jans-auth-server/config/properties.readonly"));
        System.out.println("\n\n\n UmaService::testClientPermission() - permission = "+permission+"\n\n\n\n");
        
        
        PermissionTicket ticket = this.getUmaPermissionService()
                .registerPermission("Bearer " + patToken.getAccessToken(), UmaPermissionList.instance(permission));
        
        System.out.println("\n\n\n UmaService::testClientPermission() FINAL - ticket = " + ticket + "\n\n\n\n");
        if (ticket == null) {
            return null;
        }

        // Register RPT token 
        TokenResponse tokenResponse = null;
        try {
            // rptStatusResponse = this.getUmaRptIntrospectionService().requestRptStatus(authorization,rptToken,"");
            tokenResponse = UmaClient.requestRpt("https://pujavs.jans.server/jans-auth/restv1/token", "1802.9dcd98ad-fe2c-4fd9-b717-d9436d9f2009","test1234",scopes);
            System.out.println("\n\n UmaService::getStatusResponse() - tokenResponse  = " + tokenResponse);
            System.out.println("\n\n UmaService::getStatusResponse() - tokenResponse.toString()  = " + tokenResponse.toString());
            System.out.println("\n\n UmaService::getStatusResponse() - okenResponse.getAccessToken()  = " + tokenResponse.getAccessToken());
        } catch (Exception ex) {
            log.error("Failed to determine RPT status", ex);
            ex.printStackTrace();
        }
        return ticket.getTicket();
    }

    private String getHost(String uri) throws MalformedURLException {
        URL url = new URL(uri);
        return url.getHost();
    }

}
