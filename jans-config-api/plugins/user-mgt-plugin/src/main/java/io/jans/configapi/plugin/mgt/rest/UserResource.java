package io.jans.configapi.plugin.mgt.rest;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.common.model.common.User;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.model.exception.ApiApplicationException;
import io.jans.configapi.core.util.ApiErrorResponse;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.mgt.model.user.CustomUser;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.plugin.mgt.service.UserMgmtService;
import io.jans.configapi.plugin.mgt.util.Constants;
import io.jans.configapi.plugin.mgt.util.MgtUtil;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.GluuStatus;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

import org.apache.commons.lang3.StringUtils;

@Path(Constants.CONFIG_USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource extends BaseResource {

    private static final String USER = "user";
    private static final String MAIL = "mail";
    private static final String DISPLAY_NAME = "displayName";
    private static final String GIVEN_NAME = "givenName";
    private static final String USER_PWD = "userPassword";
    private static final String INUM = "inum";
    private static final String USER_PLACEHOLDER = "user:{}";

    private class UserPagedResult extends PagedResult<CustomUser> {
    };

    @Inject
    Logger logger;

    @Inject
    MgtUtil mgtUtil;

    @Inject
    UserMgmtService userMgmtSrv;

    @Operation(summary = "Gets list of users", description = "Gets list of users", operationId = "get-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/user/user-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    public Response getUsers(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "mail=abc@mail.com,jansStatus=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "User search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

        SearchRequest searchReq = createSearchRequest(userMgmtSrv.getPeopleBaseDn(), pattern, sortBy, sortOrder,
                startIndex, limit, null, userMgmtSrv.getUserExclusionAttributesAsString(), mgtUtil.getRecordMaxCount(),
                fieldValuePair, CustomUser.class);

        return Response.ok(this.doSearch(searchReq, true)).build();
    }

    @Operation(summary = "Get User by Inum", description = "Get User by Inum", operationId = "get-user-by-inum", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "CustomUser identified by inum"), examples = @ExampleObject(name = "Response json example", value = "example/user/user.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.USER_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getUserByInum(
            @Parameter(description = "User identifier") @PathParam(ApiConstants.INUM) @NotNull String inum)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isInfoEnabled()) {
            logger.info("User search by inum:{}", escapeLog(inum));
        }
        User user = userMgmtSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        logger.debug(USER_PLACEHOLDER, user);

        // excludedAttributes
        user = excludeUserAttributes(user);
        logger.debug(USER_PLACEHOLDER, user);

        // get custom user
        CustomUser customUser = getCustomUser(user, true);
        logger.info("customUser:{}", customUser);

        return Response.ok(customUser).build();
    }

    @Operation(summary = "Create new User", description = "Create new User", operationId = "post-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS }))
    @RequestBody(description = "User object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class), examples = @ExampleObject(name = "Request json example", value = "example/user/user-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "Created Object"), examples = @ExampleObject(name = "Response json example", value = "example/user/user.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response createUser(@Valid CustomUser customUser,
            @Parameter(description = "Boolean flag to indicate if attributes to be removed for non-LDAP DB. Default value is true, indicating non-LDAP attributes will be removed from request.") @DefaultValue("true") @QueryParam(value = ApiConstants.REMOVE_NON_LDAP_ATTRIBUTES) boolean removeNonLDAPAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isInfoEnabled()) {
            logger.info("User details to be added - customUser:{}, removeNonLDAPAttributes:{}", escapeLog(customUser),
                    removeNonLDAPAttributes);
        }

        try {
            // get User object
            User user = setUserAttributes(customUser);

            // parse birthdate if present
            userMgmtSrv.parseBirthDateAttribute(user);
            logger.debug("Create  user:{}", user);

            // checking mandatory attributes
            checkMissingAttributes(user, null);
            ignoreCustomAttributes(user, removeNonLDAPAttributes);
            validateUser(user, false);
            validateAttributes(user);

            logger.info("Service call to create user:{}", user);

            user = userMgmtSrv.addUser(user, true);
            logger.info("User created {}", user);

            // excludedAttributes
            user = excludeUserAttributes(user);

            // get custom user
            customUser = getCustomUser(user, removeNonLDAPAttributes);
            logger.info("newly created customUser:{}", customUser);
        } catch (ApiApplicationException ae) {
            logger.error(ApiErrorResponse.CREATE_USER_ERROR.getDescription(), ae);
            throwBadRequestException("USER_CREATION_ERROR", ae.getMessage());
        } catch (InvalidAttributeException iae) {
            logger.error("InvalidAttributeException while creating user is:{}, cause:{}", iae, iae.getCause());
            throwBadRequestException("USER_CREATION_ERROR", iae.getMessage());
        } catch (Exception ex) {
            logger.error("Exception while creating user is:{}, cause:{}", ex, ex.getCause());
            throwInternalServerException(ex);
        }
        return Response.status(Response.Status.CREATED).entity(customUser).build();
    }

    @Operation(summary = "Update User", description = "Update User", operationId = "put-user", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS }))
    @RequestBody(description = "User object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class), examples = @ExampleObject(name = "Request json example", value = "example/user/user.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class), examples = @ExampleObject(name = "Response json example", value = "example/user/user.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    public Response updateUser(@Valid CustomUser customUser,
            @Parameter(description = "Boolean flag to indicate if attributes to be removed for non-LDAP DB. Default value is true, indicating non-LDAP attributes will be removed from request.") @DefaultValue("true") @QueryParam(value = ApiConstants.REMOVE_NON_LDAP_ATTRIBUTES) boolean removeNonLDAPAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (logger.isInfoEnabled()) {
            logger.info("User details to be updated - customUser:{}, removeNonLDAPAttributes:{}", escapeLog(customUser),
                    removeNonLDAPAttributes);
        }

        try {
            // get User object
            User user = setUserAttributes(customUser);

            // parse birthdate if present
            userMgmtSrv.parseBirthDateAttribute(user);
            logger.debug("Create  user:{}", user);

            // checking mandatory attributes
            List<String> excludeAttributes = List.of(USER_PWD);
            checkMissingAttributes(user, excludeAttributes);
            ignoreCustomAttributes(user, removeNonLDAPAttributes);
            validateUser(user, true);
            validateAttributes(user);

            logger.info("Call update user:{}", user);

            user = userMgmtSrv.updateUser(user);
            logger.info("Updated user:{}", user);

            // excludedAttributes
            user = excludeUserAttributes(user);

            // get custom user
            customUser = getCustomUser(user, removeNonLDAPAttributes);
            logger.info("updated customUser:{}", customUser);
        } catch (ApiApplicationException ae) {
            logger.error(ApiErrorResponse.UPDATE_USER_ERROR.getDescription(), ae);
            throwBadRequestException("USER_UPDATE_ERROR", ae.getMessage());
        } catch (InvalidAttributeException iae) {
            logger.error("InvalidAttributeException while updating user is:{}, cause:{}", iae, iae.getCause());
            throwBadRequestException("USER_UPDATE_ERROR", iae.getMessage());
        } catch (Exception ex) {
            logger.error("Exception while updating user is:{}, cause:{}", ex, ex.getCause());
            throwInternalServerException(ex);
        }
        return Response.ok(customUser).build();

    }

    @Operation(summary = "Patch user properties by Inum", description = "Patch user properties by Inum", operationId = "patch-user-by-inum", tags = {
            "Configuration – User Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.USER_WRITE_ACCESS }))
    @RequestBody(description = "UserPatchRequest", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserPatchRequest.class), examples = @ExampleObject(name = "Request json example", value = "example/user/user-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUser.class, description = "Patched CustomUser Object"), examples = @ExampleObject(name = "Response json example", value = "example/user/user.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @ProtectedApi(scopes = { ApiAccessConstants.USER_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchUser(
            @Parameter(description = "User identifier") @PathParam(ApiConstants.INUM) @NotNull String inum,
            @NotNull UserPatchRequest userPatchRequest,
            @Parameter(description = "Boolean flag to indicate if attributes to be removed for non-LDAP DB. Default value is true, indicating non-LDAP attributes will be removed from request.") @DefaultValue("true") @QueryParam(value = ApiConstants.REMOVE_NON_LDAP_ATTRIBUTES) boolean removeNonLDAPAttributes)
            throws IllegalAccessException, InvocationTargetException, JsonPatchException, IOException {
        if (logger.isInfoEnabled()) {
            logger.info("User:{} to be patched with :{}, removeNonLDAPAttributes:{} ", escapeLog(inum),
                    escapeLog(userPatchRequest), removeNonLDAPAttributes);
        }
        CustomUser customUser = null;
       try { 
           // check if user exists
           User existingUser = userMgmtSrv.getUserBasedOnInum(inum);

        // parse birthdate if present
        userMgmtSrv.parseBirthDateAttribute(existingUser);
        checkResourceNotNull(existingUser, USER);
        ignoreCustomAttributes(existingUser, removeNonLDAPAttributes);

        // patch user
        existingUser = userMgmtSrv.patchUser(inum, userPatchRequest);
        logger.debug("Patched user:{}", existingUser);

        // excludedAttributes
        existingUser = excludeUserAttributes(existingUser);

        // get custom user
        customUser = getCustomUser(existingUser, removeNonLDAPAttributes);
        logger.info("patched customUser:{}", customUser);
       } catch (InvalidAttributeException iae) {
           logger.error("InvalidAttributeException while updating user is:{}, cause:{}", iae, iae.getCause());
           throwBadRequestException("USER_PATCH_ERROR", iae.getMessage());
       } catch (Exception ex) {
           logger.error("Exception while pactching user is:{}, cause:{}", ex, ex.getCause());
           throwInternalServerException(ex);
       }
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
    public Response deleteUser(
            @Parameter(description = "User identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isInfoEnabled()) {
            logger.info("User to be deleted - inum:{} ", escapeLog(inum));
        }
        User user = userMgmtSrv.getUserBasedOnInum(inum);
        checkResourceNotNull(user, USER);
        userMgmtSrv.removeUser(user);
        return Response.noContent().build();
    }

    private UserPagedResult doSearch(SearchRequest searchReq, boolean removeNonLDAPAttributes)
            throws IllegalAccessException, InvocationTargetException {
        if (logger.isInfoEnabled()) {
            logger.info("User search params - searchReq:{}, removeNonLDAPAttributes:{} ", escapeLog(searchReq),
                    removeNonLDAPAttributes);
        }

        PagedResult<User> pagedResult = userMgmtSrv.searchUsers(searchReq);
        if (logger.isTraceEnabled()) {
            logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        }

        UserPagedResult pagedCustomUser = new UserPagedResult();
        if (pagedResult != null) {
            logger.debug("Users fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            List<User> users = pagedResult.getEntries();

            // excludedAttributes
            users = userMgmtSrv.excludeAttributes(users, searchReq.getExcludedAttributesStr());
            logger.debug("Users fetched  - users:{}", users);

            // parse birthdate if present
            users = users.stream().map(user -> userMgmtSrv.parseBirthDateAttribute(user)).collect(Collectors.toList());

            // get customUser()
            List<CustomUser> customUsers = getCustomUserList(users, removeNonLDAPAttributes);
            pagedCustomUser.setStart(pagedResult.getStart());
            pagedCustomUser.setEntriesCount(pagedResult.getEntriesCount());
            pagedCustomUser.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedCustomUser.setEntries(customUsers);
        }

        logger.info("User pagedCustomUser:{}", pagedCustomUser);
        return pagedCustomUser;

    }

    private User excludeUserAttributes(User user) throws IllegalAccessException, InvocationTargetException {
        return userMgmtSrv.excludeAttributes(user, userMgmtSrv.getUserExclusionAttributesAsString());
    }

    private void checkMissingAttributes(User user, List<String> excludeAttributes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ApiApplicationException {
        String missingAttributes = userMgmtSrv.checkMandatoryFields(user, excludeAttributes);
        logger.debug("missingAttributes:{}", missingAttributes);

        if (StringHelper.isEmpty(missingAttributes)) {
            return;
        }

        throw new ApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                String.format(ApiErrorResponse.MISSING_ATTRIBUTES.getDescription(), missingAttributes));
    }

    private void validateUser(User user, boolean isUpdate) throws ApiApplicationException {
        logger.info(USER_PLACEHOLDER, user);

        if (user == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();

        // check if user with same name and email already exists
        String msg = this.validateUserName(user, isUpdate);
        if (StringUtils.isNotBlank(msg)) {
            sb.append(msg);
        }
        msg = this.validateUserEmail(user, isUpdate);
        if (StringUtils.isNotBlank(msg)) {
            sb.append(msg);
        }

        if (sb.length() > 0) {
            throw new ApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    String.format(ApiErrorResponse.GENERAL_ERROR.getDescription(), sb.toString()));
        }
    }

    private String validateUserName(User user, boolean isUpdate) throws ApiApplicationException {
        logger.info(USER_PLACEHOLDER, " isUpdate:{}", user, isUpdate);

        String msg = null;

        if (user == null) {
            return msg;
        }

        // check if user with same name already exists
        final String inum = user.getAttribute("inum");
        final String name = user.getUserId();

        List<User> sameNameUser = userMgmtSrv.getUserByName(name);
        logger.info(" sameNameUser:{}", sameNameUser);

        // name validation
        if (sameNameUser != null && !sameNameUser.isEmpty()) {

            List<User> users = null;
            if (isUpdate) {
                users = sameNameUser.stream().filter(e -> !e.getAttribute("inum").equalsIgnoreCase(inum))
                        .collect(Collectors.toList());
            }

            if (!isUpdate || (users != null && !users.isEmpty())) {
                msg = String.format(ApiErrorResponse.SAME_NAME_USER_EXISTS_ERROR.getDescription(), name);
            }
        }
        return msg;
    }

    private String validateUserEmail(User user, boolean isUpdate) throws ApiApplicationException {
        logger.info(USER_PLACEHOLDER, " isUpdate:{}", user, isUpdate);

        String msg = null;

        if (user == null) {
            return msg;
        }
        // check if user with same email already exists
        final String inum = user.getAttribute("inum");
        final String email = user.getAttribute(MAIL);
        List<User> sameEmailUser = userMgmtSrv.getUserByEmail(email);
        logger.info(" sameEmailUser:{}", sameEmailUser);

        // email validation
        if (sameEmailUser != null && !sameEmailUser.isEmpty()) {

            List<User> usersList = null;
            if (isUpdate) {
                usersList = sameEmailUser.stream().filter(e -> !e.getAttribute("inum").equalsIgnoreCase(inum))
                        .collect(Collectors.toList());
            }

            if (!isUpdate || (usersList != null && !usersList.isEmpty())) {
                msg = String.format(ApiErrorResponse.SAME_NAME_EMAIL_EXISTS_ERROR.getDescription(), email);
            }
        }

        return msg;
    }

    private void validateAttributes(User user) {
        userMgmtSrv.validateAttributes(user.getCustomAttributes());
    }

    private List<CustomUser> getCustomUserList(List<User> users, boolean removeNonLDAPAttributes) {
        List<CustomUser> customUserList = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return customUserList;
        }

        for (User user : users) {
            CustomUser customUser = new CustomUser();
            setParentAttributes(customUser, user, removeNonLDAPAttributes);
            customUserList.add(customUser);
            ignoreCustomAttributes(customUser, removeNonLDAPAttributes);
        }
        logger.debug("Custom Users - customUserList:{}", customUserList);
        return customUserList;
    }

    private CustomUser getCustomUser(User user, Boolean ignoreCustomAttributes) {
        CustomUser customUser = new CustomUser();
        if (user == null) {
            return customUser;
        }
        setParentAttributes(customUser, user, ignoreCustomAttributes);
        logger.debug("Custom User - customUser:{}", customUser);
        return customUser;
    }

    public CustomUser setParentAttributes(CustomUser customUser, User user, boolean removeNonLDAPAttributes) {
        customUser.setBaseDn(user.getBaseDn());
        customUser.setCreatedAt(user.getCreatedAt());
        customUser.setCustomAttributes(user.getCustomAttributes());
        customUser.setCustomObjectClasses(user.getCustomObjectClasses());
        customUser.setDn(user.getDn());
        customUser.setOxAuthPersistentJwt(user.getOxAuthPersistentJwt());
        customUser.setUpdatedAt(user.getUpdatedAt());
        customUser.setUserId(user.getUserId());
        customUser.setStatus(user.getStatus());
        ignoreCustomAttributes(customUser, removeNonLDAPAttributes);
        return setCustomUserAttributes(customUser, user);
    }

    public CustomUser setCustomUserAttributes(CustomUser customUser, User user) {
        customUser.setMail(user.getAttribute(MAIL));
        customUser.setDisplayName(user.getAttribute(DISPLAY_NAME));
        customUser.setGivenName(user.getAttribute(GIVEN_NAME));
        customUser.setUserPassword(user.getAttribute(USER_PWD));
        customUser.setInum(user.getAttribute(INUM));
        customUser.setStatus(user.getStatus());

        customUser.removeAttribute(MAIL);
        customUser.removeAttribute(DISPLAY_NAME);
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
        user.setStatus((customUser.getStatus() != null ? customUser.getStatus() : GluuStatus.ACTIVE));

        return setUserCustomAttributes(customUser, user);
    }

    private User setUserCustomAttributes(CustomUser customUser, User user) {
        if (StringUtils.isNotBlank(customUser.getMail())) {
            user.setAttribute(MAIL, customUser.getMail(), false);
        }

        user.setAttribute(DISPLAY_NAME, customUser.getDisplayName(), false);
        user.setAttribute(GIVEN_NAME, customUser.getGivenName(), false);
        if (StringUtils.isNotBlank(customUser.getUserPassword())) {
            user.setAttribute(USER_PWD, customUser.getUserPassword(), false);
        }
        if (StringUtils.isNotBlank(customUser.getInum())) {
            user.setAttribute(INUM, customUser.getInum(), false);
        }

        return user;
    }

    private User ignoreCustomAttributes(User user, boolean removeNonLDAPAttributes) {
        logger.info(
                "\n\n ** validate User CustomObjectClasses - User user:{}, removeNonLDAPAttributes:{}, user.getCustomObjectClasses():{}, userMgmtSrv.getPersistenceType():{}, userMgmtSrv.isLDAP():?{}",
                user, removeNonLDAPAttributes, user.getCustomObjectClasses(), userMgmtSrv.getPersistenceType(),
                userMgmtSrv.isLDAP());

        if (removeNonLDAPAttributes) {
            return userMgmtSrv.ignoreCustomObjectClassesForNonLDAP(user);
        }

        return user;
    }

}