package io.jans.configapi.plugin.mgt.filters;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.core.filter.BaseFilter;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.rest.security.ScopeContext;
import io.jans.configapi.rest.security.ScopeSecurityContext;
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
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class UserResourceFilter extends BaseFilter {

    protected static final String USER_INUM = "inum";
    protected static final String PATH = "/jans-config-api/mgt/";


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
    
    @Inject 
    ScopeContext scopeContext;

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
            log.info(" Inside UserResourceFilter filter - info.getPath():{}, PATH:{}", info.getPath(), PATH);
      
            if( info.getPath() ==null || !info.getPath().contains(PATH)) {
                log.info(" Exiting as not User Management Endpoint!");
                return;
            }
            
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

        log.error("\n\n\n UserResourceFilter::authUtil.isUserRolePermissionValidationEnabled():{} {}", authUtil.isUserRolePermissionValidationEnabled(),"\n\n\n");
        // This authorization should be in addition to AuthorizationFilter authorization
        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            log.error(" UserResourceFilter - Skip validateUserRolePermission as authUtil.isUserRolePermissionValidationEnabled() not enabled");
            return;
        }
        log.error(" UserResourceFilter - authUtil.isValidateUserInumInIntrospectionFlag():{}, !getExcludedClients().isEmpty():{}", authUtil.isValidateUserInumInIntrospectionFlag(), !getExcludedClients().isEmpty());
        IntrospectionResponse introspectionResponse = null;
        if (authUtil.isValidateUserInumInIntrospectionFlag() || !getExcludedClients().isEmpty()) {
            introspectionResponse = getIntrospectionResponse(requestContext);
            log.error("\n\n\n UserResourceFilter - Setting data in requestContext \n\n\n");
            setScopeSecurityContext(requestContext, introspectionResponse);
        }
        log.error("\n\n\n UserResourceFilter - isRolePermissionExemptClient(introspectionResponse):{}", isRolePermissionExemptClient(introspectionResponse));
        if (isRolePermissionExemptClient(introspectionResponse)) {
            log.error("\n\n\n UserResourceFilter - Skip validateUserRolePermission as isRolePermissionExemptClient \n\n\n");
            return;
        }

        // For user mgt endpoint Header attribute `User-inum` is mandatory
        String userInum = authUtil.getUserInum(httpHeaders);
        log.error("\n\n\n UserResourceFilter - logged in user:{}", userInum);
        if (StringUtils.isBlank(userInum)) {
            throw new WebApplicationException("Header attribute `User-inum` missing",
                    Response.status(Response.Status.BAD_REQUEST).build());
        }

        log.error("\n\n\n UserResourceFilter - validateDataInIntrospectionResponse \n\n\n");
        validateDataInIntrospectionResponse(introspectionResponse, userInum);

        // Fetch current UserRolePermission of user using `User-inum` in httpHeaders
        Set<String> userCurrentScopes = authUtil.getUserRolePermission(httpHeaders);
        log.error("UserResourceFilter - userCurrentScopes:{}", userCurrentScopes);

        // Find missing permission viz-a-viz scopes as defined in resourceInfo
        Map<ProtectionScopeType, List<String>> resourceScopesByType = authUtil.getResourceScopesByType(resourceInfo);
        log.debug("UserResourceFilter - resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return;
        }

        List<String> resourceScopes = authUtil.getAllScopeList(resourceScopesByType);
        log.error("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

        // For any missing scopes throw unauthorized error
        List<String> safeList = new ArrayList<>(userCurrentScopes);
        List<String> missingScopes = authUtil.findMissingScopes(resourceScopesByType, safeList);
        log.error("missingScopes:{}", missingScopes);
        if (missingScopes != null && !missingScopes.isEmpty()) {
            log.error("Insufficient scopes!!! for new token as well - Required scope:{}, userCurrentScopes:{}",
                    resourceScopes, userCurrentScopes);
            throw new WebApplicationException(
                    "Insufficient scopes!!! Required scope: " + resourceScopes + ", token scopes: " + missingScopes,
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private boolean isRolePermissionExemptClient(IntrospectionResponse introspectionResponse) {

        if (introspectionResponse == null) {
            return false;
        }

        if (!getExcludedClients().contains(introspectionResponse.getClientId())) {
            return false;
        }

        String tokenUserInum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(),
                USER_INUM);
        if (StringUtils.isNotBlank(tokenUserInum)) {
            log.info("Client:{} is excluded from the user role-permission check but its token carries user:{}",
                    introspectionResponse.getClientId(), tokenUserInum);
            return false;
        }

        log.info("Skipping user role-permission check for excluded client:{}", introspectionResponse.getClientId());
        return true;
    }

    private List<String> getExcludedClients() {
        List<String> excludedClients = authUtil.getUserRolePermissionExcludedClients();
        return excludedClients == null ? Collections.emptyList() : excludedClients;
    }

    private void validateDataInIntrospectionResponse(IntrospectionResponse introspectionResponse, String userInum) {

        // validate `User-inum` in Introspection response
        if (!authUtil.isValidateUserInumInIntrospectionFlag()) {
            return;
        }

        if (introspectionResponse == null) {
            throw new WebApplicationException("Invalid token Introspection response is null.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        String inum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
        log.error("\n\n NEW Header userInum :{} and  token Introspection inum:{}", userInum, inum);
        if (StringUtils.isNotBlank(inum) && !inum.equalsIgnoreCase(userInum)) {
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
    
    private void setScopeSecurityContext(ContainerRequestContext requestContext, IntrospectionResponse introspectionResponse) {
        if(requestContext == null || introspectionResponse ==null) {
            return;
        }

        String inum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
        String subject = introspectionResponse.getSubject();
        List<String> introspectionTokenScopes = introspectionResponse.getScope();
        log.error("\n\n\n\n ******************* UserResourceFilter::setScopeSecurityContext() - inum:{}, subject:{}, introspectionTokenScopes:{}", inum, subject, introspectionTokenScopes);
        Set<String> scopes = new HashSet<>(introspectionTokenScopes);
        scopeContext.setScopes(scopes);
        scopeContext.setSubject(subject);

        // Also expose via SecurityContext for anything using @RolesAllowed-style checks
        requestContext.setSecurityContext(new ScopeSecurityContext(subject, scopes));
        
       
    }
}