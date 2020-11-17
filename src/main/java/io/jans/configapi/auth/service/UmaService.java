/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

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
    
    /*
     * public UmaService() { init(); System.out.
     * println("\n\n\n ******************* UmaService::UmaService() *******************  - Entry \n\n\n"
     * ); System.out.
     * println("\n\n\n UmaService::UmaService() - configurationService = "
     * +configurationService+" \n\n\n");
     * 
     * //getUmaMetadata();
     * 
     * //getUmaPermissionService();
     * 
     * //getUmaRptIntrospectionService();
     * System.out.println("\n\n\n UmaService::UmaService() - Exit \n\n\n"); }
     */
    
    /*
    @Override
    protected void initInternal() {
        try {
            init();
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load oxAuth UMA configuration", ex);
        }
    }

    public UmaMetadata getUmaMetadata() throws Exception {
        init();
        return this.umaMetadata;
    }
*/
   @PostConstruct
    public void init()  {
       System.out.println("\n\n\n UmaService::init() - configurationService = "+configurationService+" \n\n\n");
        this.umaMetadataService = AuthClientFactory.getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
        this.umaMetadata = umaMetadataService.getMetadata();
        this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
        this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
        
        System.out.println("\n\n\n UmaService::init() - this.umaMetadataService  = "+ this.umaMetadataService);
        System.out.println("\n\n\n UmaService::init() - this.umaMetadata  = "+ this.umaMetadata);
        System.out.println("UmaService::init() - this.umaPermissionService  = "+ this.umaPermissionService);
        System.out.println("UmaService::init() - this.umaRptIntrospectionService  = "+ this.umaRptIntrospectionService);
    }   
   
   /*
    * @Produces
    * 
    * @ApplicationScoped
    * 
    * @Named("umaMetadataService")
    */
   public UmaMetadataService getUmaMetadataService() {
       //System.out.println("\n\n\n UmaService::getUmaMetadataService() - Entry ");
       //this.umaMetadataService = AuthClientFactory
       //        .getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
      System.out.println("UmaService::getUmaMetadataService() - this.umaMetadataService = "+this.umaMetadataService);
       return this.umaMetadataService;
   }


    @Produces
    @ApplicationScoped
    @Named("umaMetadata")
    public UmaMetadata getUmaMetadata() {
        //System.out.println("\n\n\n UmaService::getUmaMetadata() - Entry ");
        //this.umaMetadata = this.getUmaMetadataService().getMetadata();
        System.out.println("\n\n\n UmaService::getUmaMetadata() - this.umaMetadata = "+this.umaMetadata+"\n\n\n");
        return this.umaMetadata;
    }
   

    /*
     * @Produces
     * 
     * @ApplicationScoped
     * 
     * @Named("umaPermissionService")
     */
    public UmaPermissionService getUmaPermissionService() {
        // System.out.println("\n\n\n UmaService::getUmaPermissionService() - Entry ");
        // this.umaPermissionService =
        // AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
        System.out.println("\n\n\n UmaService::getUmaPermissionService() - this.umaPermissionService = "+ this.umaPermissionService);
        return this.umaPermissionService;
    }

    /*
     * @Produces
     * 
     * @ApplicationScoped
     * 
     * @Named("umaRptIntrospectionService")
     */
    public UmaRptIntrospectionService getUmaRptIntrospectionService() {
        //System.out.println("\n\n\n UmaService::getUmaRptIntrospectionService() - Entry ");
        ///this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
        System.out.println("\n\n\n UmaService::getUmaRptIntrospectionService() - this.umaRptIntrospectionService  = "+this.umaRptIntrospectionService);
        return this.umaRptIntrospectionService;
    }

    /*
    @Produces
    @ApplicationScoped
    @Named("umaMetadataConfiguration")
    public UmaMetadata getUmaMetadataConfiguration() throws OxIntializationException {

        log.info("##### Getting UMA Metadata Service ...");

        UmaMetadataService umaMetadataService = AuthClientFactory
                .getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);

        log.info("##### Getting UMA Metadata ...");

        log.debug("\n\n UmaService:::::getUmaMetadataConfiguration() -  umaMetadataService =" + umaMetadataService
                + "\n\n");
        UmaMetadata umaMetadata = umaMetadataService.getMetadata();
        log.debug("\n\n UmaService:::::getUmaMetadataConfiguration() -  umaMetadata =" + umaMetadata + "\n\n");
        log.info("##### Getting UMA metadata ... DONE");

        if (umaMetadata == null) {
            throw new OxIntializationException("UMA meta data configuration is invalid!");
        }

        return umaMetadata;
    }
*/

    public void validateRptToken(Token patToken, String authorization, String resourceId, List<String> scopeIds) {
        System.out.println("\n\n UmaService::validateRptToken() - patToken  = "+patToken+" , authorization = "+authorization+" , resourceId = "+resourceId+" , scopeIds = "+scopeIds);
        log.trace("Validating RPT, resourceId: {}, scopeIds: {}, authorization: {}", resourceId, scopeIds, authorization);

        System.out.println("UmaService::validateRptToken() - this.getUmaPermissionService  = "+ this.getUmaPermissionService());
        if (patToken == null) {
            log.info("Token is blank"); // todo yuriy-> puja: it's not enough to return unauthorize, in UMA ticket has
                                        // to be registered - DONE call done Permissions ticket
            Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
            throw new WebApplicationException("Token is blank.", registerPermissionsResponse);
        }

        if (StringHelper.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
            String rptToken = authorization.substring(7);
            System.out.println("\n\n UmaService::validateRptToken() - rptToken  = "+rptToken);
            
            RptIntrospectionResponse rptStatusResponse = getStatusResponse(patToken, rptToken);
            log.trace("RPT status response: {} ", rptStatusResponse);
            
            if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
                log.warn("Status response for RPT token: '{}' is invalid, will do a retry", rptToken);
            } else {
                boolean rptHasPermissions = isRptHasPermissions(rptStatusResponse);

                if (rptHasPermissions) {
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

    private RptIntrospectionResponse getStatusResponse(Token patToken, String rptToken) {
        String authorization = "Bearer " + patToken.getAccessToken();
        System.out.println("\n\n UmaService::getStatusResponse() - authorization  = "+authorization);
        
        // Determine RPT token to status
        RptIntrospectionResponse rptStatusResponse = null;
        try {
            System.out.println("\n\n UmaService::getStatusResponse() - this.umaRptIntrospectionService  = "+this.umaRptIntrospectionService);
            rptStatusResponse = this.getUmaRptIntrospectionService().requestRptStatus(authorization, rptToken, "");
            System.out.println("\n\n UmaService::getStatusResponse() - rptStatusResponse  = "+rptStatusResponse);
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
            // return null;
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            return response;
        }
        log.debug("Construct response: HTTP 401 (Unauthorized), ticket: '{}'", ticket);

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
        System.out.println("\n\n\n UmaService::registerResourcePermission() FINAL - this.umaPermissionService = "+this.umaPermissionService+" , patToken = "+patToken+"\n\n\n\n");
        PermissionTicket ticket = this.getUmaPermissionService().registerPermission("Bearer " + patToken.getAccessToken(),
                UmaPermissionList.instance(permission));
        if (ticket == null) {
            return null;
        }
        return ticket.getTicket();
    }

    private String getHost(String uri) throws MalformedURLException {
        URL url = new URL(uri);
        return url.getHost();
    }

}
