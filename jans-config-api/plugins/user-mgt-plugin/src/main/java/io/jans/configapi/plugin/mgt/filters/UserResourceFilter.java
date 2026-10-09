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
    protected static final String PATH = "/configuser";

    @Inject
    Logger log;

    @Context
    UriInfo info;

    @Context
    HttpServletRequest servletRequest;

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
            log.info(" Inside UserResourceFilter filter - info.getPath():{}, PATH:{}", info.getPath(), PATH);

            if (info.getPath() == null || !info.getPath().contains(PATH)) {
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
            StringBuilder errMsg = new StringBuilder("UserResourceFilter - authorization failed for method:{")
                    .append(requestContext.getMethod()).append("}, path:{").append(info.getPath())
                    .append("}, errorMessage:{").append(ex.getMessage()).append("}");

            log.error(errMsg.toString());
            abortWithUnauthorized(requestContext, status, errMsg.toString());
        }
    }

    private void validateUserRolePermission(ContainerRequestContext requestContext, ResourceInfo resourceInfo,
            HttpHeaders httpHeaders) {

        log.info(
                " UserResourceFilter::validateUserRolePermission() - authUtil.isUserRolePermissionValidationEnabled():{} {}",
                authUtil.isUserRolePermissionValidationEnabled(), "");

        // If user-role-permission validation is enabled
        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            log.debug(
                    " UserResourceFilter::validateUserRolePermission() - Skip validateUserRolePermission as authUtil.isUserRolePermissionValidationEnabled() not enabled");
            return;
        }

        // Find scopes as defined in resourceInfo
        Map<ProtectionScopeType, List<String>> resourceScopesByType = authUtil.getResourceScopesByType(resourceInfo);
        log.debug("UserResourceFilter - resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return;
        }

        log.info(
                " UserResourceFilter - authUtil.isValidateUserInumInIntrospectionFlag():{}, !getExcludedClients().isEmpty():{}",
                authUtil.isValidateUserInumInIntrospectionFlag(), !getExcludedClients().isEmpty());
        // Fetch token introspectionResponse
        IntrospectionResponse introspectionResponse = getIntrospectionResponse(requestContext);
        log.info(" UserResourceFilter - Setting data in requestContext ");
        // Set Data in request context
        setDataInRequest(introspectionResponse, resourceScopesByType);

        log.info(" UserResourceFilter - isRolePermissionExemptClient(introspectionResponse):{}",
                isRolePermissionExemptClient(introspectionResponse));
        // If user excluded from role-permission check then return
        if (isRolePermissionExemptClient(introspectionResponse)) {
            log.info(" UserResourceFilter - Skip validateUserRolePermission as isRolePermissionExemptClient ");
            return;
        }

        // Fetch Header attribute `User-inum` which is mandatory for role-permission
        // check
        String userInum = authUtil.getUserInum(httpHeaders);
        log.info(" UserResourceFilter - logged in user:{}", userInum);
        if (StringUtils.isBlank(userInum)) {
            log.error("Header attribute `User-inum` missing and hence returning");
            return;
        }

        // validate IntrospectionResponse
        log.debug(" UserResourceFilter - validateDataInIntrospectionResponse ");
        validateDataInIntrospectionResponse(introspectionResponse, userInum);

        // Fetch current UserRolePermission of user using `User-inum` in httpHeaders
        List<String> currentScopes = getClientScopes(httpHeaders, introspectionResponse);
        log.debug("UserResourceFilter - currentScopes:{}", currentScopes);

        List<String> resourceScopes = authUtil.getAllScopeList(resourceScopesByType);
        log.debug("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

        // For any missing scopes throw unauthorized error
        List<String> safeList = new ArrayList<>(currentScopes);

        // Find missing permission viz-a-viz currentScopes in DB against defined in
        // resourceInfo
        List<String> missingScopes = authUtil.findMissingScopes(resourceScopesByType, safeList);
        log.debug("missingScopes:{}", missingScopes);
        if (missingScopes != null && !missingScopes.isEmpty()) {
            StringBuilder errMsg = new StringBuilder("Insufficient scopes!!! - Required scope:{").append(resourceScopes)
                    .append("} , currentScopes:{").append(currentScopes).append("} , missingScopes:{")
                    .append(missingScopes).append("}");

            log.error("UserResourceFilter::validateUserRolePermission - errMsg:{}", errMsg);
            throw new WebApplicationException(errMsg.toString(), Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private boolean isRolePermissionExemptClient(IntrospectionResponse introspectionResponse) {
        log.info("Verify isRolePermissionExemptClient");

        if (introspectionResponse == null) {
            return false;
        }

        if (!getExcludedClients().contains(introspectionResponse.getClientId())) {
            return false;
        }

        String tokenUserInum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
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
        log.info(" Verify DataInIntrospectionResponse");
        // validate `User-inum` in Introspection response
        if (!authUtil.isValidateUserInumInIntrospectionFlag()) {
            return;
        }

        if (introspectionResponse == null) {
            throw new WebApplicationException("Invalid token Introspection response is null.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        String inum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
        log.debug(" NEW Header userInum :{} and  token Introspection inum:{}", userInum, inum);
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

    private void setDataInRequest(IntrospectionResponse introspectionResponse,
            Map<ProtectionScopeType, List<String>> resourceScopesByType) {
        if (servletRequest == null || introspectionResponse == null) {
            return;
        }

        String inum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), USER_INUM);
        String subject = introspectionResponse.getSubject();
        List<String> introspectionTokenScopes = introspectionResponse.getScope();
        List<String> resourceScopes = (resourceScopesByType == null) ? null : resourceScopesByType.get(ProtectionScopeType.SUPER);
        log.info(
                "UserResourceFilter::setDataInRequest() - inum:{}, subject:{}, introspectionTokenScopes:{}, resourceScopesByType:{}, resourceScopes:{}",
                inum, subject, introspectionTokenScopes, resourceScopesByType, resourceScopes);

        servletRequest.setAttribute(ApiConstants.INTROSPECTION_SUBJECT, subject);
        servletRequest.setAttribute(ApiConstants.INTROSPECTION_SCOPES, inum);
        servletRequest.setAttribute(ApiConstants.INTROSPECTION_SCOPES, introspectionTokenScopes);
        servletRequest.setAttribute(ApiConstants.RESOURCE_SCOPES, resourceScopes);

    }

    private List<String> getClientScopes(HttpHeaders httpHeaders, IntrospectionResponse introspectionResponse) {
        List<String> scopes = null;

        try {

            scopes = authUtil.getUserRolePermission(httpHeaders);
            if (scopes != null && !scopes.isEmpty()) {
                return scopes;
            }
            log.info(" User scope:{}", scopes);

            // If user scope not found fetch client scope
            if (introspectionResponse == null) {
                return scopes;
            }
            String subject = introspectionResponse.getSubject();
            if (StringUtils.isBlank(subject)) {
                return scopes;
            }
            scopes = authUtil.getClientScope(subject);
            log.info(" scope of client:{} is:{}", subject, scopes);
        } catch (Exception ex) {
            log.error(" Error while fetching scope is: ", ex);
            return scopes;
        }
        return scopes;

    }
}