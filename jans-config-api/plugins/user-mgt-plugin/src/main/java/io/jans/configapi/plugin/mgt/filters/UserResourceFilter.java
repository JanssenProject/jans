package io.jans.configapi.plugin.mgt.filters;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.util.*;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.slf4j.Logger;

@Provider
@ProtectedApi
@Priority(200)
public class UserResourceFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_SCHEME = "Bearer";

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
            log.debug("========================================================================");
            log.error("Inside UserResourceFilter filter...");
            log.debug("========================================================================");

            log.error(
                    "\n\n\n UserResourceFilter - {} {} from IP:{}, info.getPathParameters():{}, info.getQueryParameters();{}",
                    requestContext.getMethod(), info.getPath(), request.getRemoteAddr(), info.getPathParameters(),
                    info.getQueryParameters());

            Map<String, Cookie> cookies = requestContext.getCookies();
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String issuer = requestContext.getHeaderString(ApiConstants.ISSUER);
            log.error(" UserResourceFilter data - cookies:{}, authorizationHeader:{}, issuer:{}", cookies,
                    authorizationHeader, issuer);
            // Verify current UserRolePermission
            validateUserRolePermission(resourceInfo, httpHeaders);

        } catch (Exception ex) {
            log.error("AuthorizationFilter - authorization failed for {} {}: {}", requestContext.getMethod(),
                    info.getPath(), ex.getMessage(), ex);
            abortWithUnauthorized(requestContext, ex.getMessage());
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String errMsg) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(errMsg)
                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
    }

    private void validateUserRolePermission(ResourceInfo resourceInfo, HttpHeaders httpHeaders) {
        log.error("\n\n\n validateUserRolePermission - param resourceInfo:{}, httpHeaders:{}", resourceInfo,
                httpHeaders);

        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            return;
        }

        Set<String> userCurrentScopes = authUtil.getUserRolePermission(httpHeaders);
        log.info("userCurrentScopes:{}", userCurrentScopes);

        // find missing scopes
        Map<ProtectionScopeType, List<String>> resourceScopesByType = authUtil.getResourceScopesByType(resourceInfo);
        log.error("resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return;
        }

        List<String> resourceScopes = authUtil.getAllScopeList(resourceScopesByType);
        log.debug("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

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