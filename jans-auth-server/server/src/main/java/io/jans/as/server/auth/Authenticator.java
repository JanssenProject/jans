/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.auth;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.AuthenticationService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ErrorHandlerService;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.jsf2.message.FacesMessages;
import io.jans.jsf2.service.FacesService;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.security.Credentials;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.FacesMessage.Severity;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static io.jans.as.model.config.Constants.AUTH_STEP;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Authenticator component
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version August 20, 2019
 */
@RequestScoped
@Named
public class Authenticator {

    public static final String INVALID_SESSION_MESSAGE = "login.errorSessionInvalidMessage";
    public static final String AUTHENTICATION_ERROR_MESSAGE = "login.failedToAuthenticate";
    public static final String AUTHENTICATION_SUCCESS_FOR_USER = "Authentication success for User: '{}'";

    @Inject
    private Logger logger;

    @Inject
    private Identity identity;

    @Inject
    private Credentials credentials;

    @Inject
    private ClientService clientService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private LanguageBean languageBean;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private ErrorHandlerService errorHandlerService;

    private String authAcr;

    private Integer authStep;

    private String lastResult;
    private SessionId curentSessionId;

    /**
     * Tries to authenticate an user, returns <code>true</code> if the
     * authentication succeed
     *
     * @return Returns <code>true</code> if the authentication succeed
     */
    public boolean authenticate() {
        HttpServletRequest servletRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        final SessionId sessionId = getSessionId(servletRequest);
        if (sessionIdService.isSessionIdAuthenticated(sessionId)) {
            // #1029 : session is already authenticated, we run into second authorization
            // request
            errorHandlerService.handleError("login.userAlreadyAuthenticated", AuthorizeErrorResponseType.RETRY,
                    "Session is already authenticated. Please re-send authorization request. If AS errorHandlingMethod=remote then RP can get redirect with error and re-send authorization request automatically.");
            return false;
        }

        lastResult = authenticateImpl(servletRequest, true, false, false);

        if (Constants.RESULT_SUCCESS.equals(lastResult)) {
            return true;
        } else if (Constants.RESULT_FAILURE.equals(lastResult)) {
            authenticationFailed();
        } else if (Constants.RESULT_NO_PERMISSIONS.equals(lastResult)) {
            handlePermissionsError();
        } else if (Constants.RESULT_EXPIRED.equals(lastResult)) {
            handleSessionInvalid();
        } else if (Constants.RESULT_AUTHENTICATION_FAILED.equals(lastResult)) {
            addSeverityMessages();
        }

        return false;
    }

    private void addSeverityMessages() {
        // Do nothing to keep compatibility with older versions
        if (facesMessages.getMessages().size() == 0) {
            addMessage(FacesMessage.SEVERITY_ERROR, AUTHENTICATION_ERROR_MESSAGE);
        }
    }

    public String authenticateWithOutcome() {
        HttpServletRequest servletRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        lastResult = authenticateImpl(servletRequest, true, false, false);

        if (Constants.RESULT_SUCCESS.equals(lastResult)) {
            return lastResult;
        } else if (Constants.RESULT_FAILURE.equals(lastResult)) {
            authenticationFailed();
        } else if (Constants.RESULT_NO_PERMISSIONS.equals(lastResult)) {
            handlePermissionsError();
        } else if (Constants.RESULT_EXPIRED.equals(lastResult)) {
            handleSessionInvalid();
        } else if (Constants.RESULT_AUTHENTICATION_FAILED.equals(lastResult)) {
            // Do nothing to keep compatibility with older versions
            addSeverityMessages();
            handleLoginError(null);
        }

        return lastResult;
    }

    public boolean authenticateClient(HttpServletRequest servletRequest, boolean skipPassword) {
        String result = authenticateImpl(servletRequest, false, skipPassword, true);
        return Constants.RESULT_SUCCESS.equals(result);
    }

    public boolean authenticateClient(HttpServletRequest servletRequest) {
        String result = authenticateImpl(servletRequest, false, false, true);
        return Constants.RESULT_SUCCESS.equals(result);
    }

    public boolean authenticateUser(HttpServletRequest servletRequest) {
        String result = authenticateImpl(servletRequest, false, false, false);
        return Constants.RESULT_SUCCESS.equals(result);
    }

    public boolean isServiceAuthentication(boolean service, boolean skipPassword, HttpServletRequest servletRequest) {
        if (!service) {
            return false;
        }
        return StringHelper.isNotEmpty(credentials.getUsername())
                && (skipPassword || StringHelper.isNotEmpty(credentials.getPassword())) && servletRequest != null
                && (servletRequest.getRequestURI().endsWith("/token")
                || servletRequest.getRequestURI().endsWith("/revoke")
                || servletRequest.getRequestURI().endsWith("/revoke_session")
                || servletRequest.getRequestURI().endsWith("/userinfo")
                || servletRequest.getRequestURI().endsWith("/bc-authorize")
                || servletRequest.getRequestURI().endsWith("/par")
                || servletRequest.getRequestURI().endsWith("/device_authorization"));
    }

    public String authenticateImpl(HttpServletRequest servletRequest, boolean interactive, boolean skipPassword,
                                   boolean service) {
        String result = Constants.RESULT_FAILURE;
        try {
            logger.trace("Authenticating ... (interactive: {}, skipPassword: {}, credentials.username: {})", interactive, skipPassword, credentials.getUsername());
            if (isServiceAuthentication(service, skipPassword, servletRequest)) {
                boolean authenticated = clientAuthentication(credentials, interactive, skipPassword);
                if (authenticated) {
                    result = Constants.RESULT_SUCCESS;
                }
            } else {
                if (interactive) {
                    result = userAuthenticationInteractive(servletRequest);
                } else {
                    boolean authenticated = userAuthenticationService();
                    if (authenticated) {
                        result = Constants.RESULT_SUCCESS;
                    }
                }
            }
        } catch (InvalidSessionStateException ex) {
            // Allow to handle it via GlobalExceptionHandler
            throw ex;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        if (Constants.RESULT_SUCCESS.equals(result)) {
            logger.trace("Authentication successfully for '{}'", credentials.getUsername());
            return result;
        }

        logger.info("Authentication failed for '{}'", credentials.getUsername());
        return result;
    }

    public boolean clientAuthentication(Credentials credentials, boolean interactive, boolean skipPassword) {
        boolean isServiceUsesExternalAuthenticator = !interactive
                && externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.SERVICE);
        if (isServiceUsesExternalAuthenticator) {
            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                    .determineCustomScriptConfiguration(AuthenticationScriptUsageType.SERVICE, 1, this.authAcr);

            if (customScriptConfiguration == null) {
                logger.error("Failed to get CustomScriptConfiguration. acr: '{}'", this.authAcr);
            } else {
                this.authAcr = customScriptConfiguration.getCustomScript().getName();

                boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration,
                        null, 1);
                logger.info("Authentication result for user '{}', result: '{}'", credentials.getUsername(), result);

                if (result) {
                    Client client = authenticationService.configureSessionClient();
                    showClientAuthenticationLog(client);
                    return true;
                }
            }
        }

        boolean loggedIn = skipPassword;
        if (!loggedIn) {
            loggedIn = clientService.authenticate(credentials.getUsername(), credentials.getPassword());
        }
        if (loggedIn) {
            Client client = authenticationService.configureSessionClient();
            showClientAuthenticationLog(client);
            return true;
        }

        return false;
    }

    private void showClientAuthenticationLog(Client client) {
        StringBuilder sb = new StringBuilder("Authentication success for Client");
        if (StringHelper.toBoolean(appConfiguration.getLogClientIdOnClientAuthentication(), false)
                || StringHelper.toBoolean(appConfiguration.getLogClientNameOnClientAuthentication(), false)) {
            sb.append(":");
            if (isTrue(appConfiguration.getLogClientIdOnClientAuthentication())) {
                sb.append(" ").append("'").append(client.getClientId()).append("'");
            }
            if (isTrue(appConfiguration.getLogClientNameOnClientAuthentication())) {
                sb.append(" ").append("('").append(client.getClientName()).append("')");
            }
        }
        if (logger.isInfoEnabled())
            logger.info(sb.toString());
    }

    private String userAuthenticationInteractive(HttpServletRequest servletRequest) {
        SessionId sessionId = getSessionId(servletRequest);
        Map<String, String> sessionIdAttributes = sessionIdService.getSessionAttributes(sessionId);
        if (sessionIdAttributes == null) {
            logger.debug("Unable to get session attributes. SessionId: {}", (sessionId != null ? sessionId.getId() : null));
            return Constants.RESULT_EXPIRED;
        }

        // Set current state into identity to allow use in login form and
        // authentication scripts
        identity.setSessionId(sessionId);

        initCustomAuthenticatorVariables(sessionIdAttributes);
        boolean useExternalAuthenticator = externalAuthenticationService
                .isEnabled(AuthenticationScriptUsageType.INTERACTIVE);
        if (useExternalAuthenticator && !StringHelper.isEmpty(this.authAcr)) {
            initCustomAuthenticatorVariables(sessionIdAttributes);
            if ((this.authStep == null) || StringHelper.isEmpty(this.authAcr)) {
                logger.error("Failed to determine authentication mode");
                return Constants.RESULT_EXPIRED;
            }

            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                    .getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, this.authAcr);
            if (customScriptConfiguration == null) {
                logger.error("Failed to get CustomScriptConfiguration for acr: '{}', auth_step: '{}'", this.authAcr,
                        this.authStep);
                return Constants.RESULT_FAILURE;
            }

            // Check if all previous steps had passed
            boolean passedPreviousSteps = isPassedPreviousAuthSteps(sessionIdAttributes, this.authStep);
            if (!passedPreviousSteps) {
                logger.error("There are authentication steps not marked as passed. acr: '{}', auth_step: '{}'",
                        this.authAcr, this.authStep);
                return Constants.RESULT_FAILURE;
            }

            // Restore identity working parameters from session
            setIdentityWorkingParameters(sessionIdAttributes);

            boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration,
                    externalContext.getRequestParameterValuesMap(), this.authStep);
            if (logger.isDebugEnabled()) {
                String userId = credentials.getUsername();
                if (StringHelper.isEmpty(userId)) {
                    User user = identity.getUser();
                    if (user != null) {
                        userId = user.getUserId();
                    }
                    logger.debug(
                            "Authentication result for user '{}'. auth_step: '{}', result: '{}', credentials: '{}'",
                            userId, this.authStep, result, System.identityHashCode(credentials));
                }
            }

            int overridenNextStep = -1;
            logger.trace("################## acr: {}, step: {}", authAcr, authStep);

            int apiVersion = externalAuthenticationService.executeExternalGetApiVersion(customScriptConfiguration);
            if (apiVersion > 1) {
                logger.trace("According to API version script supports steps overriding");
                overridenNextStep = externalAuthenticationService.getNextStep(customScriptConfiguration,
                        externalContext.getRequestParameterValuesMap(), this.authStep);
                logger.debug("Get next step from script: '{}'", overridenNextStep);
            }

            if (!result && (overridenNextStep == -1)) {
                // Force session lastUsedAt update if authentication attempt is failed
                sessionIdService.updateSessionId(sessionId);
                return Constants.RESULT_AUTHENTICATION_FAILED;
            }

            boolean overrideCurrentStep = false;
            if (overridenNextStep > -1) {
                overrideCurrentStep = true;

                // Reset to specified step
                sessionId = sessionIdService.resetToStep(sessionId, overridenNextStep);
                if (sessionId == null) {
                    return Constants.RESULT_AUTHENTICATION_FAILED;
                }

                this.authStep = overridenNextStep;
                logger.info("Authentication reset to step : '{}'", this.authStep);
            }

            // Update parameters map to allow access it from count
            // authentication steps method
            updateExtraParameters(customScriptConfiguration, this.authStep + 1, sessionIdAttributes);

            // Determine count authentication methods
            int countAuthenticationSteps = externalAuthenticationService.executeExternalGetCountAuthenticationSteps(customScriptConfiguration);

            sessionIdAttributes = sessionIdService.getSessionAttributes(sessionId);

            // Prepare for next step
            if ((this.authStep < countAuthenticationSteps) || overrideCurrentStep) {
                int nextStep;
                if (overrideCurrentStep) {
                    nextStep = overridenNextStep;
                } else {
                    nextStep = this.authStep + 1;
                }

                String redirectTo = externalAuthenticationService
                        .executeExternalGetPageForStep(customScriptConfiguration, nextStep);
                if (redirectTo == null) {
                    return Constants.RESULT_FAILURE;
                } else if (StringHelper.isEmpty(redirectTo)) {
                    redirectTo = "/login.xhtml";
                }

                // Store/Update extra parameters in session attributes map
                updateExtraParameters(customScriptConfiguration, nextStep, sessionIdAttributes);

                if (!overrideCurrentStep) {
                    // Update auth_step
                    sessionIdAttributes.put(AUTH_STEP, Integer.toString(nextStep));

                    // Mark step as passed
                    markAuthStepAsPassed(sessionIdAttributes, this.authStep);
                }

                if (sessionId != null) {
                    boolean updateResult = updateSession(sessionId, sessionIdAttributes);
                    if (!updateResult) {
                        return Constants.RESULT_EXPIRED;
                    }
                }

                logger.trace("Redirect to page: '{}'", redirectTo);
                facesService.redirectWithExternal(redirectTo, null);

                return Constants.RESULT_SUCCESS;
            }

            if (this.authStep == countAuthenticationSteps) {
                // Store/Update extra parameters in session attributes map
                updateExtraParameters(customScriptConfiguration, this.authStep + 1, sessionIdAttributes);

                SessionId eventSessionId = authenticationService.configureSessionUser(sessionId, sessionIdAttributes);

                authenticationService.quietLogin(credentials.getUsername());

                // Redirect to authorization workflow
                logger.debug("Sending event to trigger user redirection: '{}'", credentials.getUsername());
                authenticationService.onSuccessfulLogin(eventSessionId);

                logger.info(AUTHENTICATION_SUCCESS_FOR_USER, credentials.getUsername());
                return Constants.RESULT_SUCCESS;
            }
        } else {
            if (StringHelper.isNotEmpty(credentials.getUsername())) {
                boolean authenticated = authenticationService.authenticate(credentials.getUsername(),
                        credentials.getPassword());
                if (authenticated) {
                    SessionId eventSessionId = authenticationService.configureSessionUser(sessionId,
                            sessionIdAttributes);

                    // Redirect to authorization workflow
                    logger.debug("Sending event to trigger user redirection: '{}'", credentials.getUsername());
                    authenticationService.onSuccessfulLogin(eventSessionId);
                } else {
                    // Force session lastUsedAt update if authentication attempt is failed
                    sessionIdService.updateSessionId(sessionId);
                }

                logger.info(AUTHENTICATION_SUCCESS_FOR_USER, credentials.getUsername());
                return Constants.RESULT_SUCCESS;
            }
        }

        return Constants.RESULT_FAILURE;
    }

    protected void handleSessionInvalid() {
        errorHandlerService.handleError(INVALID_SESSION_MESSAGE,
                AuthorizeErrorResponseType.AUTHENTICATION_SESSION_INVALID,
                "Create authorization request to start new authentication session.");
    }

    protected void handleScriptError() {
        handleScriptError(AUTHENTICATION_ERROR_MESSAGE);
    }

    protected void handleScriptError(String facesMessageId) {
        errorHandlerService.handleError(facesMessageId, AuthorizeErrorResponseType.INVALID_AUTHENTICATION_METHOD,
                "Contact administrator to fix specific ACR method issue.");
    }

    protected void handlePermissionsError() {
        errorHandlerService.handleError("login.youDontHavePermission", AuthorizeErrorResponseType.ACCESS_DENIED,
                "Contact administrator to grant access to resource.");
    }

    protected void handleLoginError(String facesMessageId) {
        errorHandlerService.handleError(facesMessageId, AuthorizeErrorResponseType.LOGIN_REQUIRED,
                "User should log into into system.");
    }

    private boolean updateSession(SessionId sessionId, Map<String, String> sessionIdAttributes) {
        sessionId.setSessionAttributes(sessionIdAttributes);
        boolean updateResult = sessionIdService.updateSessionId(sessionId, true, true, true);
        if (!updateResult) {
            logger.debug("Failed to update session entry: '{}'", sessionId.getId());
            return false;
        }

        return true;
    }

    private boolean userAuthenticationService() {
        if (externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.SERVICE)) {
            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                    .determineCustomScriptConfiguration(AuthenticationScriptUsageType.SERVICE, 1, this.authAcr);

            if (customScriptConfiguration == null) {
                logger.error("Failed to get CustomScriptConfiguration. auth_step: '{}', acr: '{}'", this.authStep,
                        this.authAcr);
            } else {
                this.authAcr = customScriptConfiguration.getName();

                boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration,
                        null, 1);
                logger.info("Authentication result for '{}'. auth_step: '{}', result: '{}'", credentials.getUsername(),
                        this.authStep, result);

                if (result) {
                    authenticationService.configureEventUser();

                    logger.info(AUTHENTICATION_SUCCESS_FOR_USER, credentials.getUsername());
                    return true;
                }
                logger.info("Authentication failed for User: '{}'", credentials.getUsername());
            }
        }

        if (StringHelper.isNotEmpty(credentials.getUsername())) {
            boolean authenticated = authenticationService.authenticate(credentials.getUsername(),
                    credentials.getPassword());
            if (authenticated) {
                authenticationService.configureEventUser();

                logger.info(AUTHENTICATION_SUCCESS_FOR_USER, credentials.getUsername());
                return true;
            }
            logger.info("Authentication failed for User: '{}'", credentials.getUsername());
        }

        return false;
    }

    private void setIdentityWorkingParameters(Map<String, String> sessionIdAttributes) {
        Map<String, String> authExternalAttributes = authenticationService
                .getExternalScriptExtraParameters(sessionIdAttributes);

        HashMap<String, Object> workingParameters = identity.getWorkingParameters();
        for (Entry<String, String> authExternalAttributeEntry : authExternalAttributes.entrySet()) {
            String authExternalAttributeName = authExternalAttributeEntry.getKey();
            String authExternalAttributeType = authExternalAttributeEntry.getValue();

            if (sessionIdAttributes.containsKey(authExternalAttributeName)) {
                String authExternalAttributeValue = sessionIdAttributes.get(authExternalAttributeName);
                Object typedValue = requestParameterService.getTypedValue(authExternalAttributeValue,
                        authExternalAttributeType);

                workingParameters.put(authExternalAttributeName, typedValue);
            }
        }
    }

    public String prepareAuthenticationForStep() {
        SessionId sessionId = sessionIdService.getSessionId();
        lastResult = prepareAuthenticationForStep(sessionId);

        if (Constants.RESULT_SUCCESS.equals(lastResult)) {
            return lastResult;
        } else if (Constants.RESULT_FAILURE.equals(lastResult)) {
            handleScriptError();
        } else if (Constants.RESULT_NO_PERMISSIONS.equals(lastResult)) {
            handlePermissionsError();
        } else if (Constants.RESULT_EXPIRED.equals(lastResult)) {
            handleSessionInvalid();
        }

        return lastResult;
    }

    public String prepareAuthenticationForStep(SessionId sessionId) {
        Map<String, String> sessionIdAttributes = sessionIdService.getSessionAttributes(sessionId);
        if (sessionIdAttributes == null) {
            logger.debug("Unable to get attributes from session");
            return Constants.RESULT_EXPIRED;
        }

        // Set current state into identity to allow use in login form and
        // authentication scripts
        identity.setSessionId(sessionId);

        if (!externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.INTERACTIVE)) {
            return Constants.RESULT_SUCCESS;
        }

        initCustomAuthenticatorVariables(sessionIdAttributes);
        if (StringHelper.isEmpty(this.authAcr)) {
            return Constants.RESULT_SUCCESS;
        }

        if ((this.authStep == null) || (this.authStep < 1)) {
            return Constants.RESULT_NO_PERMISSIONS;
        }

        CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                .getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, this.authAcr);
        if (customScriptConfiguration == null) {
            logger.error("Failed to get CustomScriptConfiguration. auth_step: '{}', acr: '{}'", this.authStep,
                    this.authAcr);
            return Constants.RESULT_FAILURE;
        }

        // Check if all previous steps had passed
        boolean passedPreviousSteps = isPassedPreviousAuthSteps(sessionIdAttributes, this.authStep);
        if (!passedPreviousSteps) {
            logger.error("There are authentication steps not marked as passed. acr: '{}', auth_step: '{}'",
                    this.authAcr, this.authStep);
            return Constants.RESULT_FAILURE;
        }

        // Restore identity working parameters from session
        setIdentityWorkingParameters(sessionIdAttributes);

        String currentauthAcr = customScriptConfiguration.getName();

        customScriptConfiguration = externalAuthenticationService.determineExternalAuthenticatorForWorkflow(
                AuthenticationScriptUsageType.INTERACTIVE, customScriptConfiguration);
        if (customScriptConfiguration == null) {
            return Constants.RESULT_FAILURE;
        }

        String determinedauthAcr = customScriptConfiguration.getName();
        if (!StringHelper.equalsIgnoreCase(currentauthAcr, determinedauthAcr)) {
            // Redirect user to alternative login workflow
            String redirectTo = externalAuthenticationService
                    .executeExternalGetPageForStep(customScriptConfiguration, this.authStep);

            if (StringHelper.isEmpty(redirectTo)) {
                redirectTo = "/login.xhtml";
            }

            CustomScriptConfiguration determinedCustomScriptConfiguration = externalAuthenticationService
                    .getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, determinedauthAcr);
            if (determinedCustomScriptConfiguration == null) {
                logger.error("Failed to get determined CustomScriptConfiguration. auth_step: '{}', acr: '{}'",
                        this.authStep, this.authAcr);
                return Constants.RESULT_FAILURE;
            }

            logger.debug("Redirect to page: '{}'. Force to use acr: '{}'", redirectTo, determinedauthAcr);

            determinedauthAcr = determinedCustomScriptConfiguration.getName();
            String determinedAuthLevel = Integer.toString(determinedCustomScriptConfiguration.getLevel());

            sessionIdAttributes.put("acr", determinedauthAcr);
            sessionIdAttributes.put("auth_level", determinedAuthLevel);
            sessionIdAttributes.put(AUTH_STEP, Integer.toString(1));

            // Remove old session parameters from session
            if (isFalse(appConfiguration.getKeepAuthenticatorAttributesOnAcrChange())) {
                authenticationService.clearExternalScriptExtraParameters(sessionIdAttributes);
            }

            if (sessionId != null) {
                boolean updateResult = updateSession(sessionId, sessionIdAttributes);
                if (!updateResult) {
                    return Constants.RESULT_EXPIRED;
                }
            }

            facesService.redirectWithExternal(redirectTo, null);

            return Constants.RESULT_SUCCESS;
        }

        return executeExternalPrepareForStep(sessionId, sessionIdAttributes, customScriptConfiguration);
    }

    @NotNull
    private String executeExternalPrepareForStep(SessionId sessionId, Map<String, String> sessionIdAttributes, CustomScriptConfiguration customScriptConfiguration) {
        boolean result = externalAuthenticationService.executeExternalPrepareForStep(customScriptConfiguration,
                externalContext.getRequestParameterValuesMap(), this.authStep);
        if (result) {
            // Store/Update extra parameters in session attributes map
            updateExtraParameters(customScriptConfiguration, this.authStep, sessionIdAttributes);

            if (sessionId != null) {
                boolean updateResult = updateSession(sessionId, sessionIdAttributes);
                if (!updateResult) {
                    return Constants.RESULT_FAILURE;
                }
            }

            return Constants.RESULT_SUCCESS;
        } else {
            return Constants.RESULT_FAILURE;
        }
    }

    public boolean authenticateBySessionId(String sessionIdentifier) {
        if (StringUtils.isNotBlank(sessionIdentifier)) {
            try {
                SessionId sessionId = sessionIdService.getSessionId(sessionIdentifier);
                return authenticateBySessionId(sessionId);
            } catch (Exception e) {
                logger.trace(e.getMessage(), e);
            }
        }

        return false;
    }

    public boolean authenticateBySessionId(SessionId sessionId) {
        if (sessionId == null) {
            return false;
        }
        String sessionIdentifier = sessionId.getId();

        logger.trace("authenticateBySessionId, sessionId = '{}', session = '{}', state= '{}'", sessionIdentifier, sessionId,
                sessionId.getState());
        // IMPORTANT : authenticate by session id only if state of session is authenticated!
        if (SessionIdState.AUTHENTICATED == sessionId.getState()) {
            final User user = authenticationService.getUserOrRemoveSession(sessionId);
            if (user != null) {
                try {
                    authenticationService.quietLogin(user.getUserId());

                    authenticationService.configureEventUser(sessionId);
                } catch (Exception e) {
                    logger.trace(e.getMessage(), e);
                }

                return true;
            }
        }

        return false;
    }

    private void initCustomAuthenticatorVariables(Map<String, String> sessionIdAttributes) {
        if (sessionIdAttributes == null) {
            logger.error("Failed to restore attributes from session attributes");
            return;
        }

        this.authStep = StringHelper.toInteger(sessionIdAttributes.get(AUTH_STEP), null);
        this.authAcr = sessionIdAttributes.get(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
    }

    private boolean authenticationFailed() {
        addMessage(FacesMessage.SEVERITY_ERROR, "login.errorMessage");
        handleScriptError(null);
        return false;
    }

    private void markAuthStepAsPassed(Map<String, String> sessionIdAttributes, Integer authStep) {
        String key = String.format("auth_step_passed_%d", authStep);
        sessionIdAttributes.put(key, Boolean.TRUE.toString());
    }

    private boolean isAuthStepPassed(Map<String, String> sessionIdAttributes, Integer authStep) {
        String key = String.format("auth_step_passed_%d", authStep);
        return sessionIdAttributes.containsKey(key) && Boolean.parseBoolean(sessionIdAttributes.get(key));
    }

    private boolean isPassedPreviousAuthSteps(Map<String, String> sessionIdAttributes, Integer authStep) {
        for (int i = 1; i < authStep; i++) {
            boolean isAuthStepPassed = isAuthStepPassed(sessionIdAttributes, i);
            if (!isAuthStepPassed) {
                return false;
            }
        }

        return true;
    }

    private void updateExtraParameters(CustomScriptConfiguration customScriptConfiguration, int step,
                                       Map<String, String> sessionIdAttributes) {
        List<String> extraParameters = externalAuthenticationService
                .executeExternalGetExtraParametersForStep(customScriptConfiguration, step);
        authenticationService.updateExtraParameters(sessionIdAttributes, extraParameters);
    }

    public void configureSessionClient(Client client) {
        authenticationService.configureSessionClient(client);
    }

    public void addMessage(Severity severity, String summary) {
        String message = languageBean.getMessage(summary);
        facesMessages.add(severity, message);
    }

    private SessionId getSessionId(HttpServletRequest servletRequest) {
        if (this.curentSessionId == null && identity.getSessionId() != null) {
            curentSessionId = identity.getSessionId();
            return curentSessionId;
        }
        if (this.curentSessionId == null) {
            this.curentSessionId = sessionIdService.getSessionId(servletRequest);
        }
        return this.curentSessionId;
    }

    public String getMaskedNumber() {
        String result = getFullNumber();
        if (result.length() > 7) {
            String sub = result.substring(4, 6);
            result = result.replace(sub, "XX");
        }
        return result;
    }

    @NotNull
    private String getFullNumber() {
        String phone = null;
        SessionId sessionId = sessionIdService.getSessionId();
        if (sessionId != null) {
            phone = sessionId.getSessionAttributes().get("mobile_number");

            if (StringUtils.isBlank(phone)) {
                phone = sessionId.getSessionAttributes().get("mobile");
            }
        }
        return StringUtils.isBlank(phone) ? "UNKNOWN USER PHONE." : phone;
    }
}
