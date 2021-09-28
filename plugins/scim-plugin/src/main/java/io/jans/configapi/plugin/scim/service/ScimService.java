package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.AuthUtil;

import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Singleton
public class ScimService {

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    ConfigurationService configurationService;

    public String getIssuer() {
        return configurationService.find().getIntrospectionEndpoint();
    }

    public Response serachScimUser(String filter, Integer startIndex, Integer count,
            String sortBy, String sortOrder, String attrsList, String excludedAttrsList) throws Exception {
        log.info("Search User param passed to service -  filter:{}, startIndex:{}, sortBy:{}, sortOrder:{}, attrsList:{}, excludedAttrsList:{}", filter, startIndex, sortBy, sortOrder, attrsList, excludedAttrsList);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.searchUsers(filter, startIndex, count, sortBy, sortOrder, attrsList, excludedAttrsList);
            
        } catch (Exception ex) {
            log.error("Problems processing user-info call", ex);
            throw ex;
        }
    }
    
    public Response serachScimUserPost(SearchRequest searchRequest) throws Exception {
        log.info("Post search User param passed to service -  searchRequest:{}", searchRequest);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.searchUsersPost(searchRequest);
            
        } catch (Exception ex) {
            log.error("Problems processing user-info call", ex);
            throw ex;
        }
    }
        
    public Response createScimUser(UserResource user, String attrsList, String excludedAttrsList) throws Exception {
        log.info("To create Scim user param passed to service -  user:{}, attrsList:{}, excludedAttrsList:{}", user, attrsList, excludedAttrsList);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.createUser(user, attrsList, excludedAttrsList);
            
        } catch (Exception ex) {
            log.error("Problems while processing create-scim-user call", ex);
            throw ex;
        }
    }
    
    public Response getScimUserById(String id, String attrsList, String excludedAttrsList) throws Exception {
        log.info("To search Scim user by id param passed to service -  id:{}, attrsList:{}, excludedAttrsList:{}", id, attrsList, excludedAttrsList);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.getUserById(id, attrsList, excludedAttrsList);
            
        } catch (Exception ex) {
            log.error("Problems while processing create-scim-user call", ex);
            throw ex;
        }
    }
    
    public Response updateScimUser(UserResource user, String id,String attrsList, String excludedAttrsList) throws Exception {
        log.info("To update Scim user param passed to service -  user:{}, id:{}, attrsList:{}, excludedAttrsList:{}", user, id, attrsList, excludedAttrsList);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.updateUser(user, id, attrsList, excludedAttrsList);
            
        } catch (Exception ex) {
            log.error("Problems while processing create-scim-user call", ex);
            throw ex;
        }
    }
    
    public Response deleteScimUser(String id) throws Exception {
        log.info("To delete Scim user id passed to service -  id:{}", id);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.deleteUser(id);
            
        } catch (Exception ex) {
            log.error("Problems while processing create-scim-user call", ex);
            throw ex;
        }
    }
    
    public Response patchScimUser(PatchRequest patchRequest, String id,String attrsList, String excludedAttrsList) throws Exception {
        log.info("To patch Scim user param passed to service -  patchRequest:{}, id:{}, attrsList:{}, excludedAttrsList:{}", patchRequest, id, attrsList, excludedAttrsList);
        try {
            ClientSideService client = this.getClientSideService();           
            return client.patchUser(patchRequest, id, attrsList, excludedAttrsList);
            
        } catch (Exception ex) {
            log.error("Problems while processing create-scim-user call", ex);
            throw ex;
        }
    }
    
    private ClientSideService getClientSideService() throws Exception{
        // TO change later - Start
        String domainURL = "https://jans.server1/jans-scim/restv1";
        String OIDCMetadataUrl = "https://jans.server1/.well-known/openid-configuration";
        // - End
        return ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, authUtil.getClientId(), authUtil.getClientDecryptPassword(authUtil.getClientId()));
    }
    
    
}
