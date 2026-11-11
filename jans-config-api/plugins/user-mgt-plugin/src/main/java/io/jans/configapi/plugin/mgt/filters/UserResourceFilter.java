package io.jans.configapi.plugin.mgt.filters;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.core.filter.BaseFilter;
import io.jans.configapi.core.util.ProtectionScopeType;
import io.jans.configapi.util.*;
import io.jans.configapi.plugin.mgt.util.Constants;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class UserResourceFilter extends BaseFilter {

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
            log.info("======================== Inside UserResourceFilter filter... ========================");
            if (log.isInfoEnabled()) {
                log.info(" Inside UserResourceFilter filter - info.getPath():{}, PATH:{}, method:{}",
                        escapeLog(info.getPath()), PATH, escapeLog(requestContext.getMethod()));
            }

            if (info.getPath() == null || !info.getPath().contains(PATH)) {
                log.info(" Exiting as not User Management Endpoint!");
                return;
            }

            // Verify current UserRolePermission
            validateUserPermission(requestContext, resourceInfo);

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

    private void validateUserPermission(ContainerRequestContext requestContext, ResourceInfo resourceInfo) {

        log.info(
                " UserResourceFilter::validateUserRolePermission() - authUtil.isUserRolePermissionValidationEnabled():{} {}",
                authUtil.isUserRolePermissionValidationEnabled(), "");

        // If user-role-permission validation is not enabled return
        if (!authUtil.isUserRolePermissionValidationEnabled()) {
            log.debug(
                    " UserResourceFilter::validateUserRolePermission() - Skip validateUserRolePermission as authUtil.isUserRolePermissionValidationEnabled() not enabled");
            return;
        }

        IntrospectionResponse introspectionResponse = getIntrospectionResponse(requestContext);
        log.info(" UserResourceFilter - token introspection done- introspectionResponse:{} ", introspectionResponse);

        if (isRolePermissionExemptClient(introspectionResponse)) {
            log.info(" UserResourceFilter - Skip validateUserRolePermission as isRolePermissionExemptClient ");
            return;
        }

        // Find scopes as defined in resourceInfo
        Map<ProtectionScopeType, List<String>> resourceScopesByType = authUtil.getResourceScopesByType(resourceInfo);
        log.debug("UserResourceFilter - resourceScopesByType:{}", resourceScopesByType);
        if (resourceScopesByType == null || resourceScopesByType.isEmpty()) {
            return;
        }

        List<String> resourceScopes = authUtil.getAllScopeList(resourceScopesByType);
        log.debug("Get resourceScopesByType: {}, resourceScopes: {}", resourceScopesByType, resourceScopes);

        // Fetch current UserRolePermission of user
        List<String> userCurrentScopes = authUtil.getUserRolePermission(getUserInum(introspectionResponse));
        log.debug("userCurrentScopes:{}", userCurrentScopes);

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

        // set data in request
        setDataInRequest(introspectionResponse, resourceScopesByType);
    }

    private boolean isRolePermissionExemptClient(IntrospectionResponse introspectionResponse) {

        if (introspectionResponse == null) {
            return false;
        }

        if (!getExcludedClients().contains(introspectionResponse.getClientId())) {
            return false;
        }

        log.info("Skipping user role-permission check for excluded client:{}", introspectionResponse.getClientId());
        return true;
    }

    private List<String> getExcludedClients() {
        List<String> excludedClients = authUtil.getUserRolePermissionExcludedClients();
        return excludedClients == null ? Collections.emptyList() : excludedClients;
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

        String userInum = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(),
                Constants.USER_INUM);
        String clientId = introspectionResponse.getClientId();
        String userRole = authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(),
                Constants.USER_ROLE);
        List<String> introspectionTokenScopes = introspectionResponse.getScope();
        List<String> resourceScopes = (resourceScopesByType == null) ? null
                : resourceScopesByType.get(ProtectionScopeType.SUPER);
        log.info(
                "UserResourceFilter::setDataInRequest() - userInum:{}, clientId:{}, userRole:{}, introspectionTokenScopes:{}, resourceScopesByType:{}, resourceScopes:{}",
                userInum, clientId, userRole, introspectionTokenScopes, resourceScopesByType, resourceScopes);

        servletRequest.setAttribute(Constants.USER_INUM, userInum);
        servletRequest.setAttribute(Constants.CLIENT_ID, clientId);
        servletRequest.setAttribute(Constants.USER_ROLE, userRole);
        servletRequest.setAttribute(Constants.TOKEN_SCOPES, introspectionTokenScopes);
        servletRequest.setAttribute(Constants.RESOURCE_SCOPES, resourceScopes);

    }

    private String getUserInum(IntrospectionResponse introspectionResponse) {
        String userInum = null;
        if (introspectionResponse == null) {
            return userInum;
        }

        return authUtil.getJsonNodeKeyValue(introspectionResponse.getAuthorizationDetails(), Constants.USER_INUM);

    }

}