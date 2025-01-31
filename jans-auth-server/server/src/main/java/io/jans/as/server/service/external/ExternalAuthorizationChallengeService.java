package io.jans.as.server.service.external;

import io.jans.as.common.model.session.AuthorizationChallengeSession;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.authzchallenge.AuthorizationChallengeType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization Challenge service responsible for external script interaction.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalAuthorizationChallengeService extends ExternalScriptService {

    @Inject
    private transient AppConfiguration appConfiguration;

    @Inject
    private transient ErrorResponseFactory errorResponseFactory;

    @Inject
    private transient PersistenceEntryManager persistenceEntryManager;

    public ExternalAuthorizationChallengeService() {
        super(CustomScriptType.AUTHORIZATION_CHALLENGE);
    }

    public Map<String, String> getAuthenticationMethodClaims(ExecutionContext executionContext) {
        final List<String> acrValues = executionContext.getAuthzRequest().getAcrValuesList();
        final CustomScriptConfiguration script = identifyScript(acrValues);
        if (script == null) {
            String msg = String.format("Unable to identify script by acr_values %s.", acrValues);
            log.debug(msg);
            return new HashMap<>();
        }

        log.trace("Executing python 'getAuthenticationMethodClaims' method, script name: {}, clientId: {}",
                script.getName(), executionContext.getAuthzRequest().getClientId());

        executionContext.setScript(script);

        Map<String, String> result = new HashMap<>();
        try {
            AuthorizationChallengeType authorizationChallengeType = (AuthorizationChallengeType) script.getExternalType();
            final ExternalScriptContext scriptContext = new ExternalScriptContext(executionContext);
            result = authorizationChallengeType.getAuthenticationMethodClaims(scriptContext);

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

        log.trace("Finished 'getAuthenticationMethodClaims' method, script name: {}, clientId: {}, result: {}", script.getName(), executionContext.getAuthzRequest().getClientId(), result);

        return result;
    }

    public boolean externalAuthorize(ExecutionContext executionContext) {
        final List<String> acrValues = executionContext.getAuthzRequest().getAcrValuesList();
        final CustomScriptConfiguration script = identifyScript(acrValues);
        if (script == null) {
            String msg = String.format("Unable to identify script by acr_values %s.", acrValues);
            log.debug(msg);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, executionContext.getAuthzRequest().getState(), msg))
                    .build());
        }

        log.trace("Executing python 'authorize' method, script name: {}, clientId: {}, scope: {}, authorizationChallengeSession: {}",
                script.getName(), executionContext.getAuthzRequest().getClientId(), executionContext.getAuthzRequest().getScope(), executionContext.getAuthzRequest().getAuthorizationChallengeSession());

        executionContext.setScript(script);

        boolean result = false;
        try {
            AuthorizationChallengeType authorizationChallengeType = (AuthorizationChallengeType) script.getExternalType();
            final ExternalScriptContext scriptContext = new ExternalScriptContext(executionContext);
            result = authorizationChallengeType.authorize(scriptContext);
            saveRequestParametersInSession(scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, executionContext.getAuthzRequest().getState(), "Unable to run authorization challenge script."))
                    .build());
        }

        log.trace("Finished 'authorize' method, script name: {}, clientId: {}, result: {}", script.getName(), executionContext.getAuthzRequest().getClientId(), result);

        return result;
    }

    private void saveRequestParametersInSession(ExternalScriptContext scriptContext) {
        final AuthzRequest authzRequest = scriptContext.getAuthzRequest();
        final AuthorizationChallengeSession session = authzRequest.getAuthorizationChallengeSessionObject();
        if (session == null) {
            log.trace("Authorization challenge session is not found.");
            return;
        }

        final Map<String, String> attributes = session.getAttributes().getAttributes();
        final Map<String, String[]> parameterMap = scriptContext.getHttpRequest().getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!attributes.containsKey(entry.getKey()) && ArrayUtils.isNotEmpty(entry.getValue())) {
                final String value = entry.getValue()[0];
                attributes.put(entry.getKey(), value);
                log.trace("Put in session request parameter: {}, value: {}", entry.getKey(), value);
            }
        }

        try {
            persistenceEntryManager.merge(session);
        } catch (Exception e) {
            log.error("Failed to save authorization challenge session: " + session.getId(), e);
        }
    }

    public CustomScriptConfiguration identifyScript(List<String> acrValues) {
        log.trace("Identifying script, acr_values: {}", acrValues);

        if (acrValues == null || acrValues.isEmpty()) {
            log.trace("No acr_values, return default script");
            return getCustomScriptConfigurationByName(appConfiguration.getAuthorizationChallengeDefaultAcr());
        }

        for (String acr : acrValues) {
            final CustomScriptConfiguration script = getCustomScriptConfigurationByName(acr);
            if (script != null) {
                log.trace("Found script {} by acr {}", script.getInum(), acr);
                return script;
            }
        }

        log.trace("Unable to find script by acr_values {}", acrValues);
        return getCustomScriptConfigurationByName(appConfiguration.getAuthorizationChallengeDefaultAcr());
    }

    public void externalPrepareAuthzRequest(AuthzRequest authzRequest) {
        final List<String> acrValues = authzRequest.getAcrValuesList();
        final CustomScriptConfiguration script = identifyScript(acrValues);
        if (script == null) {
            String msg = String.format("Unable to identify script by acr_values %s.", acrValues);
            log.debug(msg);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), msg))
                    .build());
        }

        log.trace("Executing python 'prepareAuthzRequest' method, script name: {}, clientId: {}, scope: {}, authorizationChallengeSession: {}, sessionAttributes: {}",
                script.getName(), authzRequest.getClientId(), authzRequest.getScope(), authzRequest.getAuthorizationChallengeSessionAttributesSafely());

        ExecutionContext executionContext = ExecutionContext.of(authzRequest);
        executionContext.setScript(script);

        try {
            AuthorizationChallengeType authorizationChallengeType = (AuthorizationChallengeType) script.getExternalType();
            final ExternalScriptContext scriptContext = new ExternalScriptContext(executionContext);
            authorizationChallengeType.prepareAuthzRequest(scriptContext);

            scriptContext.throwWebApplicationExceptionIfSet();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace("WebApplicationException from script", e);
            }
            throw e;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, executionContext.getAuthzRequest().getState(), "Unable to run 'prepareAuthzRequest' method authorization challenge script."))
                    .build());
        }

        log.trace("Finished 'prepareAuthzRequest' method, script name: {}, clientId: {}, sessionAttributes: {}", script.getName(), executionContext.getAuthzRequest().getClientId(), authzRequest.getAuthorizationChallengeSessionAttributesSafely());
    }
}
