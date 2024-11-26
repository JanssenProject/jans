package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.configuration.AppConfiguration;
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
import org.apache.commons.lang3.StringUtils;
import org.python.google.common.collect.Lists;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static io.jans.as.model.util.StringUtils.implode;

/**
 * @author Yuriy Z
 */
@Named
public class AcrService {

    public static final String AGAMA = "agama";

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private AppConfiguration appConfiguration;

    public static boolean isAgama(String acr) {
        return StringUtils.isNotBlank(acr) && (acr.startsWith("agama_") || AGAMA.equalsIgnoreCase(acr));
    }

    public static String getScriptName(String acr) {
        return isAgama(acr) ? AGAMA : acr;
    }

    public void validateAcrs(AuthzRequest authzRequest, Client client) throws AcrChangedException {
        applyAcrMappings(authzRequest);

        checkClientAuthorizedAcrs(authzRequest, client);

        checkAcrScriptIsAvailable(authzRequest);
        checkAcrChanged(authzRequest, identity.getSessionId()); // check after redirect uri is validated
    }

    public static void removeParametersForAgamaAcr(AuthzRequest authzRequest) {
        final List<String> acrValues = authzRequest.getAcrValuesList();
        for (int i = 0; i < acrValues.size(); i++) {
            final String acr = acrValues.get(i);
            acrValues.set(i, removeParametersFromAgamaAcr(acr));
        }

        final String result = implode(acrValues, " ");
        authzRequest.setAcrValues(result);
    }

    public static String removeParametersFromAgamaAcr(String acr) {
        if (isAgama(acr)) {
            return StringUtils.substringBefore(acr, "-");
        }
        return acr;
    }

    public void checkClientAuthorizedAcrs(AuthzRequest authzRequest, Client client) {
        final List<String> authorizedAcrs = client.getAttributes().getAuthorizedAcrValues();
        if (authorizedAcrs.isEmpty()) {
            return;
        }

        final String mappedAuthorizedAcrs = applyAcrMappings(authorizedAcrs);

        for (String acr : authzRequest.getAcrValuesList()) {
            // check both actual acr and mapped (we should allow mapped values as well)
            if (!authorizedAcrs.contains(acr) && !mappedAuthorizedAcrs.contains(acr)) {
                throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.INVALID_REQUEST,
                        "Restricted acr value request, please review the list of authorized acr values for this client");
            }
        }
    }

    public void applyAcrMappings(AuthzRequest authzRequest) {
        final List<String> acrValues = authzRequest.getAcrValuesList();
        final String result = applyAcrMappings(acrValues);
        authzRequest.setAcrValues(result);
    }

    public String applyAcrMappings(List<String> acrValues) {
        final Map<String, String> mappings = appConfiguration.getAcrMappings();
        if (acrValues == null || acrValues.isEmpty()) {
            return "";
        }

        if (mappings == null || mappings.isEmpty()) {
            return implode(acrValues, " ");
        }

        boolean updated = false;
        for (int i = 0; i < acrValues.size(); i++) {
            final String acr = acrValues.get(i);
            final String value = mappings.get(acr);
            if (StringUtils.isNotBlank(value)) {
                log.debug("Replaced acr {} with {}, defined from acrMapping.", acr, value);
                acrValues.set(i, value);
                updated = true;
            }
        }

        final String result = implode(acrValues, " ");
        if (updated) {
            log.debug("Mapped result: {}", result);
        }

        return result;
    }

    public void checkAcrScriptIsAvailable(AuthzRequest authzRequest) {
        final String acrValues = authzRequest.getAcrValues();
        if (StringUtils.isBlank(acrValues)) {
            return; // nothing to validate
        }

        if (Util.isBuiltInPasswordAuthn(acrValues)) {
            return; // no need for script for built-in "simple_password_auth"
        }

        List<String> acrsToDetermineScript = getAcrsToDetermineScript(authzRequest.getAcrValuesList());
        CustomScriptConfiguration script = externalAuthenticationService.determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrsToDetermineScript);
        if (script == null) {
            String msg = String.format("Unable to find script for acr: %s. Send error: %s",
                    acrsToDetermineScript, AuthorizeErrorResponseType.UNMET_AUTHENTICATION_REQUIREMENTS.getParameter());
            log.debug(msg);
            throw authzRequest.getRedirectUriResponse().createWebException(AuthorizeErrorResponseType.UNMET_AUTHENTICATION_REQUIREMENTS, msg);
        }
    }

    public static List<String> getAcrsToDetermineScript(List<String> acrValues) {
        if (acrValues == null || acrValues.isEmpty()) {
            return Lists.newArrayList();
        }

        if (isAgama(acrValues.get(0))) {
            return Lists.newArrayList(AcrService.AGAMA);
        }

        return acrValues;
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
