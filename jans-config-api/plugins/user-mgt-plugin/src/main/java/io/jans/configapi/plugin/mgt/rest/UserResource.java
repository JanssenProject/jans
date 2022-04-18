package io.jans.configapi.plugin.mgt.rest;


import com.github.fge.jsonpatch.JsonPatchException;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.mgt.model.config.UserMgtConfigSource;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.plugin.mgt.service.UserService;
import io.jans.configapi.plugin.mgt.util.Constants;
import io.jans.configapi.plugin.mgt.util.MgtUtil;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;

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

@Path(Constants.CONFIG_USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource extends BaseResource {

    private static final String USER = "user";
    private static final String USER_PWD = "userPassword";

    @Inject
    Logger logger;
    
    @Inject
    EncryptionService encryptionService;
    
    @Inject
    MgtUtil mgtUtil;

    @Inject
    UserService userSrv;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    public Response getUsers(@DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder)
            throws EncryptionException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }
        SearchRequest searchReq = createSearchRequest(userSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder, startIndex,
                limit, null, userSrv.getUserExclusionAttributesAsString(),mgtUtil.getRecordMaxCount());

        List<User> users = this.doSearch(searchReq);
        logger.debug("User search result:{}", users);

        return Response.ok(getUsers(users)).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(@PathParam(ApiConstants.INUM) @NotNull String inum)
            throws EncryptionException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search by inum:{}", escapeLog(inum));
        }
        User user = userSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        logger.debug("user:{}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        return Response.ok(decryptUserPassword(user)).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response createUser(@Valid User user)
            throws EncryptionException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be added - user:{}", escapeLog(user));
        }

        // checking mandatory attributes
        checkMissingAttributes(user);

        user = userSrv.addUser(encryptUserPassword(user), true);
        logger.debug("User created {}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response updateUser(@Valid User user)
            throws EncryptionException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be updated - user:{}", escapeLog(user));
        }

        // checking mandatory attributes
        checkMissingAttributes(user);
        
        user = userSrv.updateUser(encryptUserPassword(user));
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
            throws EncryptionException, IllegalAccessException, InvocationTargetException, JsonPatchException, IOException {
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

        return Response.ok(decryptUserPassword(existingUser)).build();
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
    
    private List<User> getUsers(List<User> users) throws EncryptionException {
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                if (StringHelper.isNotEmpty(user.getAttribute(USER_PWD))) {
                    decryptUserPassword(user);
                }
            }
        }
        return users;
    }
    
    private User encryptUserPassword(User user) throws EncryptionException {
        if (StringHelper.isNotEmpty(user.getAttribute(USER_PWD))) {
            user.setAttribute(USER_PWD, encryptionService.encrypt(user.getAttribute(USER_PWD)), false);
        }
        return user;
    }

    private User decryptUserPassword(User user) throws EncryptionException {
        if (StringHelper .isNotEmpty(user.getAttribute(USER_PWD))) {
            user.setAttribute(USER_PWD, encryptionService.decrypt(user.getAttribute(USER_PWD)), false);
        }
        return user;
    }
    
  
}
