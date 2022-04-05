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
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.configapi.service.auth.UserService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.model.PagedResult;
import io.jans.util.security.StringEncrypter.EncryptionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
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
    EncryptionService encryptionService;

    @Inject
    UserService userSrv;

    @GET
    //@ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
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
        logger.error("User serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy), escapeLog(sortOrder));

        SearchRequest searchReq = createSearchRequest(userSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder, startIndex,
                limit, null, null);

        final List<User> users = this.doSearch(searchReq);
        logger.error("User serach result:{}", users);
        return Response.ok(getUsers(users)).build();
    }

    @GET
    //@ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User serach by inum:{}", escapeLog(inum));
        }
        User user = userSrv.getUserByInum(inum);
        logger.error("Based on inum:{}, user:{}", inum, user);
        return Response.ok(decryptUserPassword(user)).build();
    }

    @POST
    //@ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response createOpenIdConnect(@Valid User user) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be added - user:{}", escapeLog(user));
        }
        user = userSrv.addUser(encryptUserPassword(user), true);
        logger.error("User created {}", user);
        return Response.status(Response.Status.CREATED).entity(decryptUserPassword(user)).build();
    }

    @PUT
   // @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response updateUser(@Valid User user) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be updated - user:{}", escapeLog(user));
        }
        user = userSrv.updateUser(encryptUserPassword(user));
        logger.error("Updated user:{}", user);

        return Response.ok(decryptUserPassword(user)).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
   // @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchUser(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws EncryptionException, JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be patched - inum:{}, pathString:{}", escapeLog(inum), escapeLog(pathString));
        }
        User existingUser = userSrv.getUserByInum(inum);
        checkResourceNotNull(existingUser, USER);

        existingUser = Jackson.applyPatch(pathString, existingUser);
        existingUser = userSrv.updateUser(existingUser);
        logger.error("Updated user:{}", existingUser);
        return Response.ok(decryptUserPassword(existingUser)).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
   // @ProtectedApi(scopes = { ApiAccessConstants.USER_DELETE_ACCESS })
    public Response deleteUser(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("User to be deleted - inum:{} ", escapeLog(inum));
        }
        User user = userSrv.getUserByInum(inum);
        checkResourceNotNull(user, USER);
        userSrv.removeUser(user);
        return Response.noContent().build();
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

    private List<User> getUsers(List<User> users) throws EncryptionException {
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                if (StringUtils.isNotBlank(user.getAttribute("userPassword"))) {
                    decryptUserPassword(user);
                }
            }
        }
        return users;
    }

    private User encryptUserPassword(User user) throws EncryptionException {
        if (StringUtils.isNotBlank(user.getAttribute("userPassword"))) {
            //user.setAttribute("userPassword", encryptionService.encrypt(user.getAttribute("userPassword")), false);
        }
        return user;
    }

    private User decryptUserPassword(User user) throws EncryptionException {
        if (StringUtils.isNotBlank(user.getAttribute("userPassword"))) {
            //user.setAttribute("userPassword", encryptionService.decrypt(user.getAttribute("userPassword")), false);
        }
        return user;
    }

}
