package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.plugin.scim.model.config.ScimConfiguration;

import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Singleton
public class ScimService {

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    ScimConfiguration scimConfiguration;

    public Response serachScimUser(String filter, Integer startIndex, Integer count, String sortBy, String sortOrder,
            String attrsList, String excludedAttrsList) throws Exception {
              ClientSideService client = getClientSideService();
        return client.searchUsers(filter, startIndex, count, sortBy, sortOrder, attrsList, excludedAttrsList);
    }

    public Response serachScimUserPost(SearchRequest searchRequest) throws Exception {
        ClientSideService client = getClientSideService();
        return client.searchUsersPost(searchRequest);
    }

    public Response createScimUser(UserResource user, String attrsList, String excludedAttrsList) throws Exception {
        ClientSideService client = getClientSideService();
        return client.createUser(user, attrsList, excludedAttrsList);
    }

    public Response getScimUserById(String id, String attrsList, String excludedAttrsList) throws Exception {
        ClientSideService client = getClientSideService();
        return client.getUserById(id, attrsList, excludedAttrsList);

    }

    public Response updateScimUser(UserResource user, String id, String attrsList, String excludedAttrsList)
            throws Exception {
        ClientSideService client = getClientSideService();
        return client.updateUser(user, id, attrsList, excludedAttrsList);
    }

    public Response deleteScimUser(String id) throws Exception {
        ClientSideService client = getClientSideService();
        return client.deleteUser(id);
    }

    public Response patchScimUser(PatchRequest patchRequest, String id, String attrsList, String excludedAttrsList)
            throws Exception {
        ClientSideService client = getClientSideService();
        return client.patchUser(patchRequest, id, attrsList, excludedAttrsList);
    }

    private ClientSideService getClientSideService() throws Exception {
        String domainURL = authUtil.getIssuer() + scimConfiguration.getScimRelativePath();
        String oidcMetadataUrl = authUtil.getOpenIdConfigurationEndpoint();
        log.debug("Scim Client param - domainURL:{}, oidcMetadataUrl:{} ", domainURL, oidcMetadataUrl);

        return ScimClientFactory.getClient(domainURL, oidcMetadataUrl, authUtil.getClientId(),
                authUtil.getClientDecryptPassword(authUtil.getClientId()));
    }

}
