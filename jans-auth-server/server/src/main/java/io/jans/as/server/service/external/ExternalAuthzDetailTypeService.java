package io.jans.as.server.service.external;

import io.jans.as.model.authzdetails.AuthzDetail;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.authzdetails.AuthzDetailType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

import java.util.Set;

/**
 * Authz Detail custom script service. It handles one single authz detail type (not many).
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalAuthzDetailTypeService extends ExternalScriptService {

    public ExternalAuthzDetailTypeService() {
        super(CustomScriptType.AUTHZ_DETAIL);
    }

    public Set<String> getSupportedAuthzDetailsTypes() {
        return customScriptConfigurationsNameMap.keySet();
    }

    public void externalValidateAuthzDetails(AuthzRequest authzRequest) {
        ExecutionContext executionContext = ExecutionContext.of(authzRequest);
        externalValidateAuthzDetails(executionContext);
    }

    public void externalValidateAuthzDetails(ExecutionContext executionContext) {
        final AuthzDetails authzDetails = executionContext.getAuthzDetails();
        for (AuthzDetail authzDetail : authzDetails.getDetails()) {
            validateSingleAuthzDetail(executionContext, authzDetail);
        }
    }

    private void validateSingleAuthzDetail(ExecutionContext executionContext, AuthzDetail authzDetail) {
        executionContext.setAuthzDetail(authzDetail);

        final String type = authzDetail.getType();
        final CustomScriptConfiguration script = getCustomScriptConfigurationByName(type);
        if (script == null) {
            log.error("Unable to find 'AuthzDetailType' custom script by name {}", type);

            throw executionContext.getAuthzRequest().getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.ACCESS_DENIED,
                    "Unable to find 'AuthzDetailType' custom script by name " + type);
        }

        externalValidateDetail(executionContext, script);
    }

    public void externalValidateDetail(ExecutionContext executionContext, CustomScriptConfiguration script) {
        log.trace("Executing python 'validateDetail' method, script name: {}, clientId: {}, authzDetail: {}",
                script.getName(), executionContext.getClient().getClientId(), executionContext.getAuthzDetail());

        executionContext.setScript(script);

        boolean result = false;
        try {
            AuthzDetailType authzDetailType = (AuthzDetailType) script.getExternalType();
            final ExternalScriptContext scriptContext = new ExternalScriptContext(executionContext);
            result = authzDetailType.validateDetail(scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        log.trace("Finished 'validateDetail' method, script name: {}, clientId: {}, result: {}", script.getName(), executionContext.getClient().getClientId(), result);

        if (!result) {
            throw executionContext.getAuthzRequest().getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.ACCESS_DENIED,
                    "Access is denied by 'AuthzDetailType' custom script 'validateDetail' method.");
        }
    }

    public String externalGetUiRepresentation(ExecutionContext executionContext, AuthzDetail detail) {
        executionContext.setAuthzDetail(detail);

        final String type = detail.getType();
        final CustomScriptConfiguration script = getCustomScriptConfigurationByName(type);
        if (script == null) {
            log.error("Unable to find 'AuthzDetailType' custom script by name {}", type);

            return detail.getJsonObject().toString();
        }

        return externalGetUiRepresentation(executionContext, script);
    }

    public String externalGetUiRepresentation(ExecutionContext executionContext, CustomScriptConfiguration script) {
        log.trace("Executing python 'getUiRepresentation' method, script name: {}, clientId: {}, authzDetail: {}",
                script.getName(), executionContext.getAuthzRequest().getClientId(), executionContext.getAuthzDetail());

        executionContext.setScript(script);

        String result = executionContext.getAuthzDetail().toString();
        try {
            AuthzDetailType authzDetailType = (AuthzDetailType) script.getExternalType();
            final ExternalScriptContext scriptContext = new ExternalScriptContext(executionContext);
            result = authzDetailType.getUiRepresentation(scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
        }

        log.trace("Finished 'getUiRepresentation' method, script name: {}, clientId: {}, result: {}", script.getName(), executionContext.getAuthzRequest().getClientId(), result);

        return result;
    }
}
