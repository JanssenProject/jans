package io.jans.configapi.plugin.mgt.filters;

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
            validateUserRolePermission(resourceInfo, httpHeaders);

        }catch (Exception ex) {
            Response.Status status = Response.Status.UNAUTHORIZED;
            if(ex instanceof WebApplicationException) {
                status = ((WebApplicationException)ex).getResponse().getStatusInfo().toEnum();
            }

            log.error("UserResourceFilter - authorization failed for {} {}: {}", requestContext.getMethod(),
                    info.getPath(), ex.getMessage(), ex);
            abortWithUnauthorized(requestContext, status, ex.getMessage());
        }        
    }

    private void validateUserRolePermission(ResourceInfo resourceInfo, HttpHeaders httpHeaders) {

        //This authorization should be in addition to AuthorizationFilter authorization
        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            return;
        }
        
        //For user mgt endpoint Header attribute `User-inum` is mandatory
        if(StringUtils.isBlank(authUtil.getUserInum(httpHeaders))){
            throw new WebApplicationException(
                    "Header attribute `User-inum` missing", Response.status(Response.Status.BAD_REQUEST).build());
        }

        //Fetch current UserRolePermission of user using `User-inum` in httpHeaders
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

        //For any missing scopes throw unauthorized error
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
}