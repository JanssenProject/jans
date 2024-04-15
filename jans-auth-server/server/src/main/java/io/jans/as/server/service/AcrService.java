package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.util.Util;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Named
public class AcrService {

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    public void validateAcrs(AuthzRequest authzRequest, Client client) throws AcrChangedException {
        if (!client.getAttributes().getAuthorizedAcrValues().isEmpty() &&
                !client.getAttributes().getAuthorizedAcrValues().containsAll(authzRequest.getAcrValuesList())) {
            throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.INVALID_REQUEST,
                    "Restricted acr value request, please review the list of authorized acr values for this client");
        }

        checkAcrScriptIsAvailable(authzRequest);
        checkAcrChanged(authzRequest, identity.getSessionId()); // check after redirect uri is validated
    }

    public void checkAcrScriptIsAvailable(AuthzRequest authzRequest) {
        final String acrValues = authzRequest.getAcrValues();
        if (StringUtils.isBlank(acrValues)) {
            return; // nothing to validate
        }

        if (Util.isBuiltInPasswordAuthn(acrValues)) {
            return; // no need for script for built-in "simple_password_auth"
        }

        CustomScriptConfiguration script = externalAuthenticationService.determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, authzRequest.getAcrValuesList());
        if (script == null) {
            String msg = String.format("Unable to find script for acr: %s. Send error: %s",
                    acrValues, AuthorizeErrorResponseType.UNMET_AUTHENTICATION_REQUIREMENTS.getParameter());
            log.debug(msg);
            throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.UNMET_AUTHENTICATION_REQUIREMENTS, msg);
        }
    }

    private void checkAcrChanged(AuthzRequest authzRequest, SessionId sessionUser) throws AcrChangedException {
        try {
            sessionIdService.assertAuthenticatedSessionCorrespondsToNewRequest(sessionUser, authzRequest.getAcrValues());
        } catch (AcrChangedException e) { // Acr changed
            //See https://github.com/GluuFederation/oxTrust/issues/797
            if (e.isForceReAuthentication()) {
                if (!authzRequest.getPromptList().contains(Prompt.LOGIN)) {
                    log.info("ACR is changed, adding prompt=login to prompts");
                    authzRequest.addPrompt(Prompt.LOGIN);

                    sessionUser.setState(SessionIdState.UNAUTHENTICATED);
                    sessionUser.getSessionAttributes().put("prompt", authzRequest.getPrompt());
                    if (!sessionIdService.persistSessionId(sessionUser)) {
                        log.trace("Unable persist session_id, trying to update it.");
                        sessionIdService.updateSessionId(sessionUser);
                    }
                    sessionIdService.externalEvent(new SessionEvent(SessionEventType.UNAUTHENTICATED, sessionUser));
                }
            } else {
                throw e;
            }
        }
    }
}
