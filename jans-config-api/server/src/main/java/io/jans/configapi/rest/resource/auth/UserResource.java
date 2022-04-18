/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.common.User;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.user.UserPatchRequest;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.configapi.service.auth.UserService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    public Response getUsers(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }
        SearchRequest searchReq = createSearchRequest(userSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder, startIndex,
                limit, null, userSrv.getUserExclusionAttributesAsString());

        List<User> users = this.doSearch(searchReq);
        logger.debug("User search result:{}", users);

        return Response.ok(users).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(@PathParam(ApiConstants.INUM) @NotNull String inum)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search by inum:{}", escapeLog(inum));
        }
        User user = userSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        logger.debug("user:{}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        return Response.ok(user).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response createUser(@Valid User user)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be added - user:{}", escapeLog(user));
        }

        // checking mandatory attributes
        checkMissingAttributes(user);

        user = userSrv.addUser(user, true);
        logger.debug("User created {}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response updateUser(@Valid User user)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be updated - user:{}", escapeLog(user));
        }

        // checking mandatory attributes
        checkMissingAttributes(user);

        user = userSrv.updateUser((user));
        logger.debug("Updated user:{}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        return Response.ok(user).build();
    }

    @PATCH
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchUser(@PathParam(ApiConstants.INUM) @NotNull String inum,
            @NotNull UserPatchRequest userPatchRequest)
            throws IllegalAccessException, InvocationTargetException, JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("User:{} to be patched with :{} ", escapeLog(inum), escapeLog(userPatchRequest));
        }
        // check if user exists
        User existingUser = userSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(existingUser, USER);

        // patch user
        existingUser = userSrv.patchUser(inum, userPatchRequest);
        logger.debug("Patched user:{}", existingUser);

        // excludedAttributes
        existingUser = excludeUserAttributes(existingUser);

        return Response.ok(existingUser).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.USER_DELETE_ACCESS })
    public Response deleteUser(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("User to be deleted - inum:{} ", escapeLog(inum));
        }
        User user = userSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        userSrv.removeUser(user);
        return Response.noContent().build();
    }

    private List<User> doSearch(SearchRequest searchReq) throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search params - searchReq:{} ", escapeLog(searchReq));
        }

        PagedResult<User> pagedResult = userSrv.searchUsers(searchReq);
        if (logger.isTraceEnabled()) {
            logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        }

        List<User> users = new ArrayList<>();
        if (pagedResult != null) {
            logger.debug("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            users = pagedResult.getEntries();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Users fetched  - users:{}", users);
        }

        // excludedAttributes
        users = userSrv.excludeAttributes(users, searchReq.getExcludedAttributesStr());

        return users;
    }

    private User excludeUserAttributes(User user) throws IllegalAccessException, InvocationTargetException {
        return userSrv.excludeAttributes(user, userSrv.getUserExclusionAttributesAsString());
    }

    private void checkMissingAttributes(User user)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String missingAttributes = userSrv.checkMandatoryFields(user);
        logger.debug("missingAttributes:{}", missingAttributes);

        if (StringHelper.isEmpty(missingAttributes)) {
            return;
        }

        throwMissingAttributeError(missingAttributes);
    }
}
