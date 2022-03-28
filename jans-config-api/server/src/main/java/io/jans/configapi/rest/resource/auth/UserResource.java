/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.InumService;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.configapi.service.auth.UserService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;


@Path(ApiConstants.USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource extends BaseResource {
    
    private static final String USER = "user";

    @Inject
    Logger logger;

    @Inject
    UserService userSrv;
    
    @Inject
    ConfigurationService configurationService;
    
    @Inject
    private InumService inumService;

    @Inject
    EncryptionService encryptionService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    public Response getOpenIdConnectClients(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        SearchRequest searchReq = createSearchRequest(userSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder,
                startIndex, limit, null, null);

        final List<User> users = this.doSearch(searchReq);
        logger.trace("User serach result:{}", users);
        return Response.ok(getUsers(users)).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("User serach by inum:{}", escapeLog(inum));
        }
        User user = userSrv.getUserByInum(inum);
        checkResourceNotNull(user, USER);
        return Response.ok(user).build();
    }

   /* @POST
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    public Response createOpenIdConnect(@Valid User user) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be added - user:{}", escapeLog(user));
        }
        String inum = user.getUserId();
        if (inum == null || inum.isEmpty() || inum.isBlank()) {
            inum = inumService.generateClientInum();
            user.setUserId(inum);
        }
        checkNotNull(user., AttributeNames.DISPLAY_NAME);
        String clientSecret = client.getClientSecret();

        if (StringHelper.isEmpty(clientSecret)) {
            clientSecret = generatePassword();
        }

        client.setClientSecret(encryptionService.encrypt(clientSecret));
        client.setDn(userSrv.getDnForClient(inum));
        client.setDeletable(client.getClientSecretExpiresAt() != null);
        ignoreCustomObjectClassesForNonLDAP(client);       
        
        logger.debug("Final Client details to be added - client:{}", client);      
        userSrv.addClient(user);
        User result = userSrv.getUserByInum(inum);
        result.setClientSecret(encryptionService.decrypt(result.getClientSecret()));

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    public Response updateUser(@Valid User user) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be updated - user:{}", escapeLog(user));
        }
        String inum = client.getClientId();
        checkNotNull(inum, AttributeNames.INUM);
        checkNotNull(client.getClientName(), AttributeNames.DISPLAY_NAME);
        Client existingClient = userSrv.getClientByInum(inum);
        checkResourceNotNull(existingClient, USER);
        client.setClientId(existingClient.getClientId());
        client.setBaseDn(userSrv.getDnForClient(inum));
        client.setDeletable(client.getExpirationDate() != null);
        if (client.getClientSecret() != null) {
            client.setClientSecret(encryptionService.encrypt(client.getClientSecret()));
        }
        ignoreCustomObjectClassesForNonLDAP(client);
   
        logger.debug("Final Client details to be updated - user:{}", user);      
        userSrv.updateClient(client);
        User result = userSrv.getClientByInum(existingClient.getClientId());
        result.setClientSecret(encryptionService.decrypt(client.getClientSecret()));

        return Response.ok(result).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchUser(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be patched - inum:{}, pathString:{}", escapeLog(inum),
                    escapeLog(pathString));
        }
        User existingUser = userSrv.getClientByInum(inum);
        checkResourceNotNull(existingUser, USER);

        existingUser = Jackson.applyPatch(pathString, existingUser);
        userSrv.updateClient(existingUser);
        return Response.ok(existingUser).build();
    }

*/
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS })
    public Response deleteUser(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("User to be deleted - inum:{} ", escapeLog(inum));
        }
        User user = userSrv.getUserByInum(inum);
        checkResourceNotNull(user, USER);
        userSrv.removeUser(user);
        return Response.noContent().build();
    }

    private List<User> getUsers(List<User> users) throws EncryptionException {
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                //user.setClientSecret(encryptionService.decrypt(user.));
            }
        }
        return users;
    }

    private String generatePassword() {
        return UUID.randomUUID().toString();
    }

    private List<User> doSearch(SearchRequest searchReq) {
        if (logger.isDebugEnabled()) {
            logger.debug("User search params - searchReq:{} ", escapeLog(searchReq));
        }

        PagedResult<User> pagedResult = userSrv.searchUsers(searchReq);
        if (logger.isTraceEnabled()) {
            logger.trace("PagedResult  - pagedResult:{}", pagedResult);
        }

        List<User> users = new ArrayList<>();
        if (pagedResult != null) {
            logger.trace("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            users = pagedResult.getEntries();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Users fetched  - users:{}", users);
        }
        return users;
    }
    

}
