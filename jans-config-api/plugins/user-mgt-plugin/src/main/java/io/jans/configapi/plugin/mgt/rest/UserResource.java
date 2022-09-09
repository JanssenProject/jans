package io.jans.configapi.plugin.mgt.rest;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.mgt.model.user.CustomUser;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.plugin.mgt.service.UserMgmtService;
import io.jans.configapi.plugin.mgt.util.Constants;
import io.jans.configapi.plugin.mgt.util.MgtUtil;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.jans.as.model.util.Util.escapeLog;

@Path(Constants.CONFIG_USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource extends BaseResource {

    private static final String USER = "user";
    private static final String MAIL = "mail";
    private static final String DISPLAY_NAME = "displayName";
    private static final String JANS_STATUS = "jansStatus";
    private static final String GIVEN_NAME = "givenName";
    private static final String USER_PWD = "userPassword";
    private static final String INUM = "inum";

    @Inject
    Logger logger;

    @Inject
    EncryptionService encryptionService;

    @Inject
    MgtUtil mgtUtil;

    @Inject
    UserMgmtService userMgmtSrv;

    @Operation(summary = "Gets list of users", description = "Gets list of users", operationId = "get-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = CustomUser.class, description = "List of CustomUser")))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    public Response getUsers(
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }
        SearchRequest searchReq = createSearchRequest(userMgmtSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder,
                startIndex, limit, null, userMgmtSrv.getUserExclusionAttributesAsString(), mgtUtil.getRecordMaxCount());

        List<CustomUser> customUsers = this.doSearch(searchReq);
        logger.debug("CustomUser search result:{}", customUsers);

        return Response.ok(customUsers).build();
    }

    @Operation(summary = "Get User by Inum", description = "Get User by Inum", operationId = "get-user-by-inum", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "CustomUser identified by inum"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(@PathParam(ApiConstants.INUM) @NotNull String inum)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search by inum:{}", escapeLog(inum));
        }
        User user = userMgmtSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        logger.debug("user:{}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);
        logger.debug("user:{}", user);

        // get custom user
        CustomUser customUser = getCustomUser(user);
        logger.debug("customUser:{}", customUser);

        return Response.ok(customUser).build();
    }

    @Operation(summary = "Create new User", description = "Create new User", operationId = "post-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS}))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "Created Object"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response createUser(@Valid CustomUser customUser)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be added - customUser:{}", escapeLog(customUser));
        }

        // get User object
        User user = setUserAttributes(customUser);
        // parse birthdate if present
        userMgmtSrv.parseBirthDateAttribute(user);
        logger.debug("Create  user:{}", user);

        // checking mandatory attributes
        checkMissingAttributes(user, null);

        user = userMgmtSrv.addUser(user, true);
        logger.debug("User created {}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        // get custom user
        customUser = getCustomUser(user);
        logger.debug("newly created customUser:{}", customUser);

        return Response.status(Response.Status.CREATED).entity(customUser).build();
    }

    @Operation(summary = "Update User", description = "Update User", operationId = "put-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response updateUser(@Valid CustomUser customUser)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User details to be updated - customUser:{}", escapeLog(customUser));
        }

        // get User object
        User user = setUserAttributes(customUser);
        // parse birthdate if present
        userMgmtSrv.parseBirthDateAttribute(user);
        logger.debug("Create  user:{}", user);

        // checking mandatory attributes
        List<String> excludeAttributes = List.of(USER_PWD);
        checkMissingAttributes(user, excludeAttributes);

        user = userMgmtSrv.updateUser(user);
        logger.debug("Updated user:{}", user);

        // excludedAttributes
        user = excludeUserAttributes(user);

        // get custom user
        customUser = getCustomUser(user);
        logger.debug("updated customUser:{}", customUser);

        return Response.ok(customUser).build();
    }

    @Operation(summary = "Patch user properties by Inum", description = "Patch user properties by Inum", operationId = "patch-user-by-inum", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS  }))
    @RequestBody(description = "UserPatchRequest", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = UserPatchRequest.class), examples = {
            @ExampleObject(value = "[ {\"jsonPatchString\": {\"op\": \"add\", \"path\": \"userId\",\"value\": \"test-user\" }, \"customAttributes\": [{\"name\": \"name, displayName, birthdate, email\",\"multiValued\": true,\"values\": [\"string\"]}]}]") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "Patched CustomUser Object"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
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
        User existingUser = userMgmtSrv.getUserBasedOnInum(inum);
        // parse birthdate if present
        userMgmtSrv.parseBirthDateAttribute(existingUser);
        checkResourceNotNull(existingUser, USER);

        // patch user
        existingUser = userMgmtSrv.patchUser(inum, userPatchRequest);
        logger.debug("Patched user:{}", existingUser);

        // excludedAttributes
        existingUser = excludeUserAttributes(existingUser);

        // get custom user
        CustomUser customUser = getCustomUser(existingUser);
        logger.debug("patched customUser:{}", customUser);

        return Response.ok(customUser).build();
    }

    @Operation(summary = "Delete User", description = "Delete User", operationId = "delete-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.USER_DELETE_ACCESS })
    public Response deleteUser(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("User to be deleted - inum:{} ", escapeLog(inum));
        }
        User user = userMgmtSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        userMgmtSrv.removeUser(user);
        return Response.noContent().build();
    }

    private List<CustomUser> doSearch(SearchRequest searchReq)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("User search params - searchReq:{} ", escapeLog(searchReq));
        }

        PagedResult<User> pagedResult = userMgmtSrv.searchUsers(searchReq);
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
        users = userMgmtSrv.excludeAttributes(users, searchReq.getExcludedAttributesStr());
        logger.debug("Users fetched  - users:{}", users);

        // parse birthdate if present
        users = users.stream().map(user -> userMgmtSrv.parseBirthDateAttribute(user)).collect(Collectors.toList());

        // get customUser()
        return getCustomUserList(users);
    }

    private User excludeUserAttributes(User user) throws IllegalAccessException, InvocationTargetException {
        return userMgmtSrv.excludeAttributes(user, userMgmtSrv.getUserExclusionAttributesAsString());
    }

    private void checkMissingAttributes(User user, List<String> excludeAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String missingAttributes = userMgmtSrv.checkMandatoryFields(user, excludeAttributes);
        logger.debug("missingAttributes:{}", missingAttributes);

        if (StringHelper.isEmpty(missingAttributes)) {
            return;
        }

        throwMissingAttributeError(missingAttributes);
    }

    private List<CustomUser> getCustomUserList(List<User> users) {
        List<CustomUser> customUserList = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return customUserList;
        }

        for (User user : users) {
            CustomUser customUser = new CustomUser();
            setParentAttributes(customUser, user);
            customUserList.add(customUser);
        }
        logger.debug("Custom Users - customUserList:{}", customUserList);
        return customUserList;
    }

    private CustomUser getCustomUser(User user) {
        CustomUser customUser = new CustomUser();
        if (user == null) {
            return customUser;
        }
        setParentAttributes(customUser, user);
        logger.debug("Custom User - customUser:{}", customUser);
        return customUser;
    }

    public CustomUser setParentAttributes(CustomUser customUser, User user) {
        customUser.setBaseDn(user.getBaseDn());
        customUser.setCreatedAt(user.getCreatedAt());
        customUser.setCustomAttributes(user.getCustomAttributes());
        customUser.setCustomObjectClasses(user.getCustomObjectClasses());
        customUser.setDn(user.getDn());
        customUser.setOxAuthPersistentJwt(user.getOxAuthPersistentJwt());
        customUser.setUpdatedAt(user.getUpdatedAt());
        customUser.setUserId(user.getUserId());

        return setCustomUserAttributes(customUser, user);
    }

    public CustomUser setCustomUserAttributes(CustomUser customUser, User user) {
        customUser.setMail(user.getAttribute(MAIL));
        customUser.setDisplayName(user.getAttribute(DISPLAY_NAME));
        customUser.setJansStatus(user.getAttribute(JANS_STATUS));
        customUser.setGivenName(user.getAttribute(GIVEN_NAME));
        customUser.setUserPassword(user.getAttribute(USER_PWD));
        customUser.setInum(user.getAttribute(INUM));

        customUser.removeAttribute(MAIL);
        customUser.removeAttribute(DISPLAY_NAME);
        customUser.removeAttribute(JANS_STATUS);
        customUser.removeAttribute(GIVEN_NAME);
        customUser.removeAttribute(USER_PWD);
        customUser.removeAttribute(INUM);

        return customUser;
    }

    private User setUserAttributes(CustomUser customUser) {
        User user = new User();
        user.setBaseDn(customUser.getBaseDn());
        user.setCreatedAt(customUser.getCreatedAt());
        user.setCustomAttributes(customUser.getCustomAttributes());
        user.setCustomObjectClasses(customUser.getCustomObjectClasses());
        user.setDn(customUser.getDn());
        user.setOxAuthPersistentJwt(customUser.getOxAuthPersistentJwt());
        user.setUpdatedAt(customUser.getUpdatedAt());
        user.setUserId(customUser.getUserId());
        return setUserCustomAttributes(customUser, user);
    }

    private User setUserCustomAttributes(CustomUser customUser, User user) {
        user.setAttribute(MAIL, customUser.getMail(), false);
        user.setAttribute(DISPLAY_NAME, customUser.getDisplayName(), false);
        user.setAttribute(JANS_STATUS, customUser.getJansStatus(), false);
        user.setAttribute(GIVEN_NAME, customUser.getGivenName(), false);
        user.setAttribute(USER_PWD, customUser.getUserPassword(), false);
        user.setAttribute(INUM, customUser.getInum(), false);

        logger.debug("Custom User - user:{}", user);
        return user;
    }
}
