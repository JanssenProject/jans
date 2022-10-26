/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.service.external.ExternalUmaClaimsGatheringService;
import io.jans.as.server.uma.authorization.UmaGatherContext;
import io.jans.jsf2.service.FacesService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz
 * @version August 9, 2017
 */
@RequestScoped
@Named(value = "gatherer")
public class UmaGatherer {

    private final Map<String, String> pageClaims = new HashMap<>();
    @Inject
    private Logger log;
    @Inject
    private ExternalUmaClaimsGatheringService external;
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
    private UmaSessionService umaSessionService;
    @Inject
    private UmaPermissionService umaPermissionService;
    @Inject
    private UmaPctService umaPctService;
    @Inject
    private UserService userService;

    public static String addQueryParameters(String url, String parameters) {
        if (StringUtils.isNotBlank(parameters)) {
            if (url.contains("?")) {
                url += "&" + parameters;
            } else {
                url += "?" + parameters;
            }
        }
        return url;
    }

    public static String addQueryParameter(String url, String paramName, String paramValue) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        if (StringUtils.isNotBlank(paramValue)) {
            if (url.contains("?")) {
                url += "&" + paramName + "=" + paramValue;
            } else {
                url += "?" + paramName + "=" + paramValue;
            }
        }
        return url;
    }

    public boolean gather() {
        try {
            final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();
            final SessionId session = umaSessionService.getSession(httpRequest, httpResponse);

            CustomScriptConfiguration script = getScript(session);
            UmaGatherContext context = new UmaGatherContext(script.getConfigurationAttributes(), httpRequest, session, umaSessionService, umaPermissionService,
                    umaPctService, pageClaims, appConfiguration);

            int step = umaSessionService.getStep(session);
            if (!umaSessionService.isPassedPreviousSteps(session, step)) {
                log.error("There are claims-gathering steps not marked as passed. scriptName: '{}', step: '{}'", script.getName(), step);
                return false;
            }

            boolean gatheredResult = external.gather(script, step, context);
            log.debug("Claims-gathering result for script '{}', step: '{}', gatheredResult: '{}'", script.getName(), step, gatheredResult);

            int overridenNextStep = external.getNextStep(script, step, context);

            if (!gatheredResult && overridenNextStep == -1) {
                return false;
            }

            if (overridenNextStep != -1) {
                umaSessionService.resetToStep(session, overridenNextStep, step);
                step = overridenNextStep;
            }

            int stepsCount = external.getStepsCount(script, context);

            if (step < stepsCount || overridenNextStep != -1) {
                int nextStep;
                if (overridenNextStep != -1) {
                    nextStep = overridenNextStep;
                } else {
                    nextStep = step + 1;
                    umaSessionService.markStep(session, step, true);
                }

                umaSessionService.setStep(nextStep, session);
                context.persist();

                String page = external.getPageForStep(script, nextStep, context);

                log.trace("Redirecting to page: '{}'", page);
                facesService.redirect(page);
                return true;
            }

            if (step == stepsCount) {
                context.persist();
                onSuccess(session, context);
                return true;
            }
        } catch (Exception e) {
            log.error("Exception during gather() method call.", e);
        }

        log.error("Failed to perform gather() method successfully.");
        return false;
    }

    private void onSuccess(SessionId session, UmaGatherContext context) {
        List<UmaPermission> permissions = context.getPermissions();
        String newTicket = umaPermissionService.changeTicket(permissions, permissions.get(0).getAttributes());

        String url = constructRedirectUri(session, context, newTicket);
        if (StringUtils.isNotBlank(url)) {
            facesService.redirectToExternalURL(url);
        } else {
            log.debug("Redirect to claims_redirect_uri is skipped because it was not provided during request.");
        }
    }

    private String constructRedirectUri(SessionId session, UmaGatherContext context, String newTicket) {
        String claimsRedirectUri = umaSessionService.getClaimsRedirectUri(session);
        if (StringUtils.isBlank(claimsRedirectUri)) {
            log.debug("claims_redirect_uri is blank, session: {}", session);
            return "";
        }

        claimsRedirectUri = addQueryParameters(claimsRedirectUri, context.getRedirectUserParameters().buildQueryString().trim());
        claimsRedirectUri = addQueryParameter(claimsRedirectUri, "state", umaSessionService.getState(session));
        claimsRedirectUri = addQueryParameter(claimsRedirectUri, "ticket", newTicket);
        return claimsRedirectUri;
    }

    public String prepareForStep() {
        try {
            final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
            final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();
            final SessionId session = umaSessionService.getSession(httpRequest, httpResponse);

            if (session == null || session.getSessionAttributes().isEmpty()) {
                log.error("Invalid session.");
                return result(Constants.RESULT_EXPIRED);
            }

            CustomScriptConfiguration script = getScript(session);
            if (script == null) {
                log.error("Failed to load script, session: '{}'", session.getId());
                return result(Constants.RESULT_FAILURE);
            }

            UmaGatherContext context = new UmaGatherContext(script.getConfigurationAttributes(), httpRequest, session, umaSessionService, umaPermissionService,
                    umaPctService, pageClaims, appConfiguration);

            int step = umaSessionService.getStep(session);
            if (step < 1) {
                log.error("Invalid step: {}", step);
                return result(Constants.RESULT_INVALID_STEP);
            }

            if (!umaSessionService.isPassedPreviousSteps(session, step)) {
                log.error("There are claims-gathering steps not marked as passed. scriptName: '{}', step: '{}'", script.getName(), step);
                return result(Constants.RESULT_FAILURE);
            }

            boolean result = external.prepareForStep(script, step, context);
            if (result) {
                context.persist();
                return result(Constants.RESULT_SUCCESS);
            } else {
                String redirectToExternalUrl = context.getRedirectToExternalUrl();
                if (StringUtils.isNotBlank(redirectToExternalUrl)) {
                    log.debug("Redirect to : {}", redirectToExternalUrl);
                    facesService.redirectToExternalURL(redirectToExternalUrl);
                    return redirectToExternalUrl;
                }
            }
        } catch (Exception e) {
            log.error("Failed to prepareForStep()", e);
        }
        return result(Constants.RESULT_FAILURE);
    }

    public String result(String resultCode) {
        if (Constants.RESULT_FAILURE.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "uma2.gather.failed");
        } else if (Constants.RESULT_INVALID_STEP.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "uma2.invalid.step");
        } else if (Constants.RESULT_EXPIRED.equals(resultCode)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "uma2.invalid.session");
        }
        return resultCode;
    }

    public void addMessage(FacesMessage.Severity severity, String summary) {
        String msg = languageBean.getMessage(summary);
        FacesMessage message = new FacesMessage(severity, msg, null);
        facesContext.addMessage(null, message);
    }

    public Map<String, String> getPageClaims() {
        return pageClaims;
    }

    protected CustomScriptConfiguration getScript(final SessionId session) {
        String scriptName = umaSessionService.getScriptName(session);
        return external.getCustomScriptConfigurationByName(scriptName);
    }

}
