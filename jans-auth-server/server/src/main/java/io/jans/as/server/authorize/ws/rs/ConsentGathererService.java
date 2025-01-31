/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.StringUtils;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.service.AuthorizeService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalConsentGatheringService;
import io.jans.as.server.service.external.context.ConsentGatheringContext;
import io.jans.jsf2.service.FacesService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Movchan Date: 10/30/2017
 */
@RequestScoped
@Named(value = "consentGatherer")
public class ConsentGathererService {

    @Inject
    private Logger log;

    @Inject
    private ExternalConsentGatheringService external;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private FacesService facesService;

    @Inject
    private LanguageBean languageBean;

    @Inject
    private ConsentGatheringSessionService sessionService;

    @Inject
    private UserService userService;

    @Inject
    private AuthorizeService authorizeService;

    @Inject
    private ClientService clientService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ScopeChecker scopeChecker;

    private final Map<String, String> pageAttributes = new HashMap<>();
    private ConsentGatheringContext context;

    public boolean configure(String userDn, String clientId, String state, List<String> acrValues) {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();

        final SessionId session = sessionService.getConsentSession(httpRequest, httpResponse, userDn, true);

        CustomScriptConfiguration script = determineConsentScript(clientId, acrValues);
        if (script == null) {
            log.error("Failed to determine consent-gathering script");
            return false;
        }

        sessionService.configure(session, script.getName(), clientId, state, acrValues);

        this.context = new ConsentGatheringContext(script.getConfigurationAttributes(), httpRequest, httpResponse, session,
                pageAttributes, sessionService, userService, facesService, appConfiguration);
        log.debug("Configuring consent-gathering script '{}'", script.getName());

        int step = sessionService.getStep(session);
        String redirectTo = external.getPageForStep(script, step, context);
        if (StringHelper.isEmpty(redirectTo)) {
            log.error("Failed to determine page for consent-gathering script");
            return false;
        }

        context.persist();

        log.trace("Redirecting to page: '{}'", redirectTo);
        facesService.redirectWithExternal(redirectTo, null);

        return true;
    }

    private CustomScriptConfiguration determineConsentScript(String clientId, List<String> acrValues) {
        log.trace("Trying to determine consent script, clientId {}, acrValues {} ...", clientId, acrValues);

        if (isTrue(appConfiguration.getConsentGatheringScriptBackwardCompatibility())) {
            // in 4.1 and earlier we returned default consent script
            log.trace("determineConsentScript - falled back to default script {}", external.getDefaultExternalCustomScript().getName());
            return external.getDefaultExternalCustomScript();
        }

        final CustomScriptConfiguration consentScriptByAcr = findConsentScriptByAcr(acrValues);
        if (consentScriptByAcr != null) {
            return consentScriptByAcr;
        }

        final List<String> consentGatheringScripts = clientService.getClient(clientId).getAttributes().getConsentGatheringScripts();
        final List<CustomScriptConfiguration> scripts = external.getCustomScriptConfigurationsByDns(consentGatheringScripts);
        if (!scripts.isEmpty()) {
            final CustomScriptConfiguration script = Collections.max(scripts, Comparator.comparingInt(CustomScriptConfiguration::getLevel)); // flow supports single script, thus taking the one with higher level
            log.debug("Determined consent gathering script `{}`", script.getName());
            return script;
        }

        log.debug("There no consent gathering script configured for client `{}`. Therefore taking default consent script.", clientId);
        return external.getDefaultExternalCustomScript();
    }

    public CustomScriptConfiguration findConsentScriptByAcr(List<String> acrValues) {
        final Map<String, String> acrToConsentScriptMap = appConfiguration.getAcrToConsentScriptNameMapping();
        if (acrToConsentScriptMap.isEmpty()) {
            log.trace("findConsentScriptByAcr - 'acrToConsentScriptNameMapping' configuration property is empty");
            return null;
        }

        for (Map.Entry<String, String> entry : acrToConsentScriptMap.entrySet()) {
            for (String acr : acrValues) {
                if (entry.getKey().equalsIgnoreCase(acr)) {
                    final String scriptName = entry.getValue();
                    log.trace("Found mapping to consent script {}, acr {}", scriptName, acr);
                    final CustomScriptConfiguration script = external.getCustomScriptConfigurationByName(scriptName);
                    if (script != null) {
                        log.trace("Found consent script by name {}, id {}", scriptName, script.getInum());
                        return script;
                    } else {
                        log.trace("Unable to find consent script by name {}", scriptName);
                    }
                }
            }
        }

        log.trace("findConsentScriptByAcr - unable to find consent script, acr: {}, acrToConsentScriptNameMapping: {}", acrValues, acrToConsentScriptMap);
        return null;
    }

    public boolean authorize() {
        try {
            final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();

            final SessionId session = sessionService.getConsentSession(httpRequest, httpResponse, null, false);
            if (session == null) {
                log.error("Failed to restore claim-gathering session state");
                errorPage("consent.gather.invalid.session");
                return false;
            }

            CustomScriptConfiguration script = getScript(session);
            if (script == null) {
                log.error("Failed to find script '{}' in session:", sessionService.getScriptName(session));
                errorPage("consent.gather.failed");
                return false;
            }

            int step = sessionService.getStep(session);
            if (!sessionService.isPassedPreviousSteps(session, step)) {
                log.error("There are consent-gathering steps not marked as passed. scriptName: '{}', step: '{}'", script.getName(), step);
                errorPage("consent.gather.invalid.step");
                return false;
            }

            this.context = new ConsentGatheringContext(script.getConfigurationAttributes(), httpRequest, httpResponse, session,
                    pageAttributes, sessionService, userService, facesService, appConfiguration);
            boolean authorizeResult = external.authorize(script, step, context);
            log.debug("Consent-gathering result for script '{}', step: '{}', gatheredResult: '{}'", script.getName(), step, authorizeResult);

            int overridenNextStep = external.getNextStep(script, step, context);
            if (!authorizeResult && overridenNextStep == -1) {
                SessionId connectSession = sessionService.getConnectSession(httpRequest);
                authorizeService.permissionDenied(connectSession);
                return false;
            }

            if (overridenNextStep != -1) {
                sessionService.resetToStep(session, overridenNextStep, step);
                step = overridenNextStep;
            }

            int stepsCount = external.getStepsCount(script, context);
            if (step < stepsCount || overridenNextStep != -1) {
                int nextStep;
                if (overridenNextStep != -1) {
                    nextStep = overridenNextStep;
                } else {
                    nextStep = step + 1;
                    sessionService.markStep(session, step, true);
                }

                sessionService.setStep(nextStep, session);

                String redirectTo = external.getPageForStep(script, nextStep, context);
                context.persist();

                log.trace("Redirecting to page: '{}'", redirectTo);
                facesService.redirectWithExternal(redirectTo, null);

                return true;
            }

            if (step == stepsCount) {
                context.persist();
                onSuccess(httpRequest, session, context);
                return true;
            }
        } catch (Exception e) {
            log.error("Exception during gather() method call.", e);
        }

        log.error("Failed to perform gather() method successfully.");
        errorPage("consent.gather.failed");
        return false;
    }

    private void onSuccess(HttpServletRequest httpRequest, SessionId session, ConsentGatheringContext context) {
        sessionService.setAuthenticatedSessionState(httpRequest, context.getHttpResponse(), session);

        SessionId connectSessionId = sessionService.getConnectSession(httpRequest);

        authorizeService.permissionGranted(httpRequest, connectSessionId);
    }

    public String prepareForStep() {
        try {
            final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();

            final SessionId session = sessionService.getConsentSession(httpRequest, httpResponse, null, false);
            if (session == null || session.getSessionAttributes().isEmpty()) {
                log.error("Failed to restore claim-gathering session state");
                return result(Constants.RESULT_EXPIRED);
            }

            CustomScriptConfiguration script = getScript(session);
            if (script == null) {
                log.error("Failed to find script '{}' in session:", sessionService.getScriptName(session));
                return result(Constants.RESULT_FAILURE);
            }

            int step = sessionService.getStep(session);
            if (step < 1) {
                log.error("Invalid step: {}", step);
                return result(Constants.RESULT_INVALID_STEP);
            }

            if (!sessionService.isPassedPreviousSteps(session, step)) {
                log.error("There are consent-gathering steps not marked as passed. scriptName: '{}', step: '{}'", script.getName(), step);
                return result(Constants.RESULT_FAILURE);
            }

            this.context = new ConsentGatheringContext(script.getConfigurationAttributes(), httpRequest, httpResponse, session,
                    pageAttributes, sessionService, userService, facesService, appConfiguration);
            boolean result = external.prepareForStep(script, step, context);
            log.debug("Consent-gathering prepare for step result for script '{}', step: '{}', gatheredResult: '{}'", script.getName(), step, result);
            if (result) {
                context.persist();
                return result(Constants.RESULT_SUCCESS);
            }
        } catch (Exception ex) {
            log.error("Failed to prepareForStep()", ex);
        }

        return result(Constants.RESULT_FAILURE);
    }

    private void errorPage(String errorKey) {
        addMessage(FacesMessage.SEVERITY_ERROR, errorKey);
        facesService.redirect("/error.xhtml");
    }

    public String result(String resultCode) {
        if (Constants.RESULT_FAILURE.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "consent.gather.failed");
        } else if (Constants.RESULT_INVALID_STEP.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "consent.gather.invalid.step");
        } else if (Constants.RESULT_EXPIRED.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "consent.gather.invalid.session");
        }
        return resultCode;
    }

    public void addMessage(FacesMessage.Severity severity, String summary) {
        String msg = languageBean.getMessage(summary);
        FacesMessage message = new FacesMessage(severity, msg, null);
        facesContext.addMessage(null, message);
    }

    public Map<String, String> getPageAttributes() {
        return pageAttributes;
    }

    protected CustomScriptConfiguration getScript(final SessionId session) {
        String scriptName = sessionService.getScriptName(session);
        return external.getCustomScriptConfigurationByName(scriptName);
    }

    public boolean isConsentGathered() {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        return sessionService.isSessionStateAuthenticated(httpRequest);
    }

    public ConsentGatheringContext getContext() {
        return context;
    }

    public List<Scope> getScopes() {
        if (context == null) {
            return Collections.emptyList();
        }

        SessionId authenticatedSessionId = sessionIdService.getSessionId();
        // Fix the list of scopes in the authorization page. oxAuth #739
        Set<String> grantedScopes = scopeChecker.checkScopesPolicy(context.getClient(), authenticatedSessionId.getSessionAttributes().get(AuthorizeRequestParam.SCOPE));
        String allowedScope = StringUtils.implode(grantedScopes, " ");

        return authorizeService.getScopes(allowedScope);
    }

}
