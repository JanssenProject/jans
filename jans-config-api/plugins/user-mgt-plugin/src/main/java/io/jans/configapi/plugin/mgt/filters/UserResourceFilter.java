package io.jans.configapi.plugin.mgt.filters;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.core.filter.BaseFilter;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.util.*;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class UserResourceFilter extends BaseFilter {

    protected static final String USER_INUM = "inum";

    @Inject
    Logger log;

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    private AuthUtil authUtil;

    /**
     * Additional User Management requests by extracting a user details and
     * validating user-role-permission
     *
     * @param requestContext the JAX-RS request context whose headers may be
     *                       modified or whose request may be aborted
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            log.info("======================== Inside UserResourceFilter filter... ========================");

            // Verify current UserRolePermission
            validateUserRolePermission(requestContext, resourceInfo, httpHeaders);

        } catch (Exception ex) {
            Response.Status status = Response.Status.UNAUTHORIZED;
            if (ex instanceof WebApplicationException) {
                status = ((WebApplicationException) ex).getResponse().getStatusInfo().toEnum();
            }

            log.error("UserResourceFilter - authorization failed for {} {}: {}", requestContext.getMethod(),
                    info.getPath(), ex.getMessage(), ex);
            abortWithUnauthorized(requestContext, status, ex.getMessage());
        }
    }

    private void validateUserRolePermission(ContainerRequestContext requestContext, ResourceInfo resourceInfo,
            HttpHeaders httpHeaders) {

        // This authorization should be in addition to AuthorizationFilter authorization
        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            return;
        }

        // For user mgt endpoint Header attribute `User-inum` is mandatory
        String userInum = authUtil.getUserInum(httpHeaders);
        if (StringUtils.isBlank(userInum)) {
            throw new WebApplicationException("Header attribute `User-inum` missing",
                    Response.status(Response.Status.BAD_REQUEST).build());
        }

        validateDataInIntrospectionResponse(requestContext, userInum);

        // Fetch current UserRolePermission of user using `User-inum` in httpHeaders
        Set<String> userCurrentScopes = authUtil.getUserRolePermission(httpHeaders);
        log.debug("userCurrentScopes:{}", userCurrentScopes);

        // Find missing permission viz-a-viz scopes as defined in resourceInfo
        Map<ProtectionScopeType, List<String>> resourceScopesByType = authUtil.getResourceScopesByType(resourceInfo);
        log.debug("resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return;
        }

        List<String> resourceScopes = authUtil.getAllScopeList(resourceScopesByType);
        log.debug("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

        // For any missing scopes throw unauthorized error
        List<String> safeList = new ArrayList<>(userCurrentScopes);
        List<String> missingScopes = authUtil.findMissingScopes(resourceScopesByType, safeList);
        log.info("missingScopes:{}", missingScopes);
        if (missingScopes != null && !missingScopes.isEmpty()) {
            log.error("Insufficient scopes!!! for new token as well - Required scope:{}, userCurrentScopes:{}",
                    resourceScopes, userCurrentScopes);
            throw new WebApplicationException(
                    "Insufficient scopes!!! Required scope: " + resourceScopes + ", token scopes: " + missingScopes,
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private void validateDataInIntrospectionResponse(ContainerRequestContext context, String userInum) {

        // validate `User-inum` in Introspection response
        if (!authUtil.isValidateUserInumInIntrospectionFlag()) {
            return;
        }

        IntrospectionResponse introspectionResponse = getIntrospectionResponse(context);

        if (introspectionResponse == null) {
            throw new WebApplicationException("Invalid token Introspection response is null.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        String inum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
        log.debug("Header userInum :{} and  token Introspection inum:{}", userInum, inum);
        if (StringUtils.isBlank(inum) || !inum.equalsIgnoreCase(userInum)) {
            throw new WebApplicationException("Header attribute `User-inum` does not correspond to User token",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private IntrospectionResponse getIntrospectionResponse(ContainerRequestContext context) {

        String authorizationHeader = getAuthorizationHeader(context);
        IntrospectionResponse introspectionResponse = null;
        try {
            introspectionResponse = authUtil.getIntrospectionResponse(authorizationHeader);
        } catch (Exception ex) {
            log.error("Error while token Introspection", ex);
            throw new WebApplicationException("Error while token Introspection.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return introspectionResponse;
    }
}