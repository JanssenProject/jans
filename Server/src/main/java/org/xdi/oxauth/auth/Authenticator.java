/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.auth;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.service.FacesService;
import org.slf4j.Logger;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.security.Credentials;
import org.xdi.oxauth.security.Identity;
import org.xdi.oxauth.security.SimplePrincipal;
import org.xdi.oxauth.service.AuthenticationService;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.util.StringHelper;

/**
 * Authenticator component
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version December 15, 2015
 */
@RequestScoped
@Named
public class Authenticator {

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private Credentials credentials;

    @Inject
    private ClientService clientService;

    @Inject
    private SessionStateService sessionStateService;

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

    private String authAcr;

    private Integer authStep;

    private boolean addedErrorMessage;

    /**
     * Tries to authenticate an user, returns <code>true</code> if the
     * authentication succeed
     *
     * @return Returns <code>true</code> if the authentication succeed
     */
    public boolean authenticate() {
        if (!authenticateImpl(true, false)) {
            return authenticationFailed();
        } else {
            return true;
        }
    }

    public String authenticateWithOutcome() {
        boolean result = authenticateImpl(true, false);
        if (result) {
            return Constants.RESULT_SUCCESS;
        } else {
            return Constants.RESULT_FAILURE;
        }

    }

    public boolean authenticateWebService(boolean skipPassword) {
        return authenticateImpl(false, skipPassword);
    }

    public boolean authenticateWebService() {
        return authenticateImpl(false, false);
    }

    public boolean authenticateImpl(boolean interactive, boolean skipPassword) {
        boolean authenticated = false;
        try {
            log.trace("Authenticating ... (interactive: " + interactive + ", skipPassword: " + skipPassword + ", credentials.username: " + credentials.getUsername() + ")");
            if (StringHelper.isNotEmpty(credentials.getUsername()) && (skipPassword || StringHelper.isNotEmpty(credentials.getPassword()))
                    && credentials.getUsername().startsWith("@!")) {
                authenticated = clientAuthentication(credentials, interactive, skipPassword);
            } else {
                if (interactive) {
                    authenticated = userAuthenticationInteractive();
                } else {
                    authenticated = userAuthenticationService();
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        if (authenticated) {
            log.trace("Authentication successfully for '{}'", credentials.getUsername());
            return true;
        }

        log.info("Authentication failed for '{}'", credentials.getUsername());
        return false;
    }

    private boolean clientAuthentication(Credentials credentials, boolean interactive, boolean skipPassword) {
        boolean isServiceUsesExternalAuthenticator = !interactive && externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.SERVICE);
        if (isServiceUsesExternalAuthenticator) {
            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                    .determineCustomScriptConfiguration(AuthenticationScriptUsageType.SERVICE, 1, this.authAcr);

            if (customScriptConfiguration == null) {
                log.error("Failed to get CustomScriptConfiguration. acr: '{}'", this.authAcr);
            } else {
                this.authAcr = customScriptConfiguration.getCustomScript().getName();

                boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration, null, 1);
                log.info("Authentication result for user '{}', result: '{}'", credentials.getUsername(), result);

                if (result) {
                    authenticationService.configureSessionClient();

                    log.info("Authentication success for client: '{}'", credentials.getUsername());
                    return true;
                }
            }
        }

        boolean loggedIn = skipPassword;
        if (!loggedIn) {
            loggedIn = clientService.authenticate(credentials.getUsername(), credentials.getPassword());
        }
        if (loggedIn) {
            authenticationService.configureSessionClient();

            log.info("Authentication success for Client: '{}'", credentials.getUsername());
            return true;
        }

        return false;
    }

    private boolean userAuthenticationInteractive() {
        SessionState sessionState = sessionStateService.getSessionState();
        Map<String, String> sessionIdAttributes = sessionStateService.getSessionAttributes(sessionState);
        if (sessionIdAttributes == null) {
            log.error("Failed to get session attributes");
            authenticationFailedSessionInvalid();
            return false;
        }

        initCustomAuthenticatorVariables(sessionIdAttributes);
        boolean useExternalAuthenticator = externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.INTERACTIVE);
        if (useExternalAuthenticator && !StringHelper.isEmpty(this.authAcr)) {
            initCustomAuthenticatorVariables(sessionIdAttributes);
            if ((this.authStep == null) || StringHelper.isEmpty(this.authAcr)) {
                log.error("Failed to determine authentication mode");
                authenticationFailedSessionInvalid();
                return false;
            }

            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, this.authAcr);
            if (customScriptConfiguration == null) {
                log.error("Failed to get CustomScriptConfiguration for acr: '{}', auth_step: '{}'", this.authAcr, this.authStep);
                return false;
            }

            // Check if all previous steps had passed
            boolean passedPreviousSteps = isPassedPreviousAuthSteps(sessionIdAttributes, this.authStep);
            if (!passedPreviousSteps) {
                log.error("There are authentication steps not marked as passed. acr: '{}', auth_step: '{}'", this.authAcr, this.authStep);
                return false;
            }

            boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration, externalContext.getRequestParameterValuesMap(), this.authStep);
            log.debug("Authentication result for user '{}'. auth_step: '{}', result: '{}', credentials: '{}'", credentials.getUsername(), this.authStep, result, System.identityHashCode(credentials));

            int overridenNextStep = -1;

            int apiVersion = externalAuthenticationService.executeExternalGetApiVersion(customScriptConfiguration);
            if (apiVersion > 1) {
            	log.trace("According to API version script supports steps overriding");
            	overridenNextStep = externalAuthenticationService.getNextStep(customScriptConfiguration, externalContext.getRequestParameterValuesMap(), this.authStep);
            	log.debug("Get next step from script: '{}'", apiVersion);
            }

            if (!result && (overridenNextStep == -1)) {
                return false;
            }

            boolean overrideCurrentStep = false;
            if (overridenNextStep > -1) {
            	overrideCurrentStep = true;
            	// Reload session state
                sessionState = sessionStateService.getSessionState();

                // Reset to pecified step
            	sessionStateService.resetToStep(sessionState, overridenNextStep);

            	this.authStep = overridenNextStep;
            	log.info("Authentication reset to step : '{}'", this.authStep);
            }


            // Update parameters map to allow access it from count authentication steps method
            updateExtraParameters(customScriptConfiguration, this.authStep + 1, sessionIdAttributes);

            // Determine count authentication methods
            int countAuthenticationSteps = externalAuthenticationService.executeExternalGetCountAuthenticationSteps(customScriptConfiguration);

            // Reload from LDAP to make sure that we are updating latest session attributes
            sessionState = sessionStateService.getSessionState();
            sessionIdAttributes = sessionStateService.getSessionAttributes(sessionState);

            // Prepare for next step
            if (this.authStep < countAuthenticationSteps) {
            	int nextStep;
            	if (overrideCurrentStep) {
            		nextStep = overridenNextStep;
            	} else {
            		nextStep = this.authStep + 1;
            	}

            	String redirectTo = externalAuthenticationService.executeExternalGetPageForStep(customScriptConfiguration, nextStep);
                if (StringHelper.isEmpty(redirectTo)) {
                	redirectTo = "/login.xhtml";
                }

                // Store/Update extra parameters in session attributes map
                updateExtraParameters(customScriptConfiguration, nextStep, sessionIdAttributes);

                if (!overrideCurrentStep) {
	                // Update auth_step
	                sessionIdAttributes.put("auth_step", Integer.toString(nextStep));

	                // Mark step as passed
	                markAuthStepAsPassed(sessionIdAttributes, this.authStep);
                }

                if (sessionState != null) {
                	boolean updateResult = updateSession(sessionState, sessionIdAttributes);
                	if (!updateResult) {
                		return false;
                	}
                }

                log.trace("Redirect to page: '{}'", redirectTo);
                facesService.redirect(redirectTo);
                return true;
            }

            if (this.authStep == countAuthenticationSteps) {
            	SessionState eventSessionState = authenticationService.configureSessionUser(sessionState, sessionIdAttributes);

                Principal principal = new SimplePrincipal(credentials.getUsername());
                identity.acceptExternallyAuthenticatedPrincipal(principal);
                identity.quietLogin();

                // Redirect to authorization workflow
                log.debug("Sending event to trigger user redirection: '{}'", credentials.getUsername());
                authenticationService.onSuccessfulLogin(eventSessionState);
//                    Events.instance().raiseEvent(Constants.EVENT_OXAUTH_CUSTOM_LOGIN_SUCCESSFUL);

                log.info("Authentication success for User: '{}'", credentials.getUsername());
                return true;
            }
        } else {
            if (StringHelper.isNotEmpty(credentials.getUsername())) {
                boolean authenticated = authenticationService.authenticate(credentials.getUsername(), credentials.getPassword());
                if (authenticated) {
                	SessionState eventSessionState = authenticationService.configureSessionUser(sessionState, sessionIdAttributes);

                    // Redirect to authorization workflow
                        log.debug("Sending event to trigger user redirection: '{}'", credentials.getUsername());
                        authenticationService.onSuccessfulLogin(eventSessionState);
// TODO: CDI Review                       
//                        Events.instance().raiseEvent(Constants.EVENT_OXAUTH_CUSTOM_LOGIN_SUCCESSFUL);
                    }

                    log.info("Authentication success for User: '{}'", credentials.getUsername());
                    return true;
            }
        }

        return false;
    }

	private boolean updateSession(SessionState sessionState, Map<String, String> sessionIdAttributes) {
		sessionState.setSessionAttributes(sessionIdAttributes);
		boolean updateResult = sessionStateService.updateSessionState(sessionState, true, true, true);
		if (!updateResult) {
		    log.debug("Failed to update session entry: '{}'", sessionState.getId());
		    return false;
		}

		return true;
	}

    private boolean userAuthenticationService() {
        if (externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.SERVICE)) {
            CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService
                    .determineCustomScriptConfiguration(AuthenticationScriptUsageType.SERVICE, 1, this.authAcr);

            if (customScriptConfiguration == null) {
                log.error("Failed to get CustomScriptConfiguration. auth_step: '{}', acr: '{}'",
                        this.authStep, this.authAcr);
            } else {
                this.authAcr = customScriptConfiguration.getName();

                boolean result = externalAuthenticationService.executeExternalAuthenticate(customScriptConfiguration, null, 1);
                log.info("Authentication result for '{}'. auth_step: '{}', result: '{}'", credentials.getUsername(), this.authStep, result);

                if (result) {
                    authenticationService.configureEventUser();

                    log.info("Authentication success for User: '{}'", credentials.getUsername());
                    return true;
                }
                log.info("Authentication failed for User: '{}'", credentials.getUsername());
            }
        }

        if (StringHelper.isNotEmpty(credentials.getUsername())) {
            boolean authenticated = authenticationService.authenticate(credentials.getUsername(), credentials.getPassword());
            if (authenticated) {
                authenticationService.configureEventUser();

                log.info("Authentication success for User: '{}'", credentials.getUsername());
                return true;
            }
            log.info("Authentication failed for User: '{}'", credentials.getUsername());
        }

        return false;
    }

	private void updateExtraParameters(CustomScriptConfiguration customScriptConfiguration, final int step, Map<String, String> sessionIdAttributes) {
		List<String> extraParameters = externalAuthenticationService.executeExternalGetExtraParametersForStep(customScriptConfiguration, step);
		if (extraParameters != null) {
		    for (String extraParameter : extraParameters) {
		    	if (authenticationService.isParameterExists(extraParameter)) {
			        String extraParameterValue = authenticationService.getParameterValue(extraParameter);
			        sessionIdAttributes.put(extraParameter, extraParameterValue);
		    	}
		    }
		}
	}

    public String prepareAuthenticationForStep() {
        SessionState sessionState = sessionStateService.getSessionState();
        Map<String, String> sessionIdAttributes = sessionStateService.getSessionAttributes(sessionState);
        if (sessionIdAttributes == null) {
            log.error("Failed to get attributes from session");
            return Constants.RESULT_EXPIRED;
        }

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

        CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.getCustomScriptConfiguration(
                AuthenticationScriptUsageType.INTERACTIVE, this.authAcr);
        if (customScriptConfiguration == null) {
            log.error("Failed to get CustomScriptConfiguration. auth_step: '{}', acr: '{}'", this.authStep, this.authAcr);
            return Constants.RESULT_FAILURE;
        }

        String currentauthAcr = customScriptConfiguration.getName();

        customScriptConfiguration = externalAuthenticationService.determineExternalAuthenticatorForWorkflow(
                AuthenticationScriptUsageType.INTERACTIVE, customScriptConfiguration);
        if (customScriptConfiguration == null) {
            return Constants.RESULT_FAILURE;
        } else {
            String determinedauthAcr = customScriptConfiguration.getName();
            if (!StringHelper.equalsIgnoreCase(currentauthAcr, determinedauthAcr)) {
                // Redirect user to alternative login workflow
                String redirectTo = externalAuthenticationService.executeExternalGetPageForStep(customScriptConfiguration, this.authStep);

                if (StringHelper.isEmpty(redirectTo)) {
                    redirectTo = "/login.xhtml";
                }

                CustomScriptConfiguration determinedCustomScriptConfiguration = externalAuthenticationService.getCustomScriptConfiguration(
                        AuthenticationScriptUsageType.INTERACTIVE, determinedauthAcr);
                if (determinedCustomScriptConfiguration == null) {
                    log.error("Failed to get determined CustomScriptConfiguration. auth_step: '{}', acr: '{}'", this.authStep, this.authAcr);
                    return Constants.RESULT_FAILURE;
                }

                log.debug("Redirect to page: '{}'. Force to use acr: '{}'", redirectTo, determinedauthAcr);

                determinedauthAcr = determinedCustomScriptConfiguration.getName();
                String determinedAuthLevel = Integer.toString(determinedCustomScriptConfiguration.getLevel());

                sessionIdAttributes.put("acr", determinedauthAcr);
                sessionIdAttributes.put("auth_level", determinedAuthLevel);
                sessionIdAttributes.put("auth_step", Integer.toString(1));

                if (sessionState != null) {
                	boolean updateResult = updateSession(sessionState, sessionIdAttributes);
                    if (!updateResult) {
                        return Constants.RESULT_EXPIRED;
                    }
                }

                facesService.redirect(redirectTo);

                return Constants.RESULT_SUCCESS;
            }
        }

        // Check if all previous steps had passed
        boolean passedPreviousSteps = isPassedPreviousAuthSteps(sessionIdAttributes, this.authStep);
        if (!passedPreviousSteps) {
            log.error("There are authentication steps not marked as passed. acr: '{}', auth_step: '{}'", this.authAcr, this.authStep);
            return Constants.RESULT_FAILURE;
        }

        Boolean result = externalAuthenticationService.executeExternalPrepareForStep(customScriptConfiguration, externalContext.getRequestParameterValuesMap(), this.authStep);
        if ((result != null) && result) {
            // Store/Update extra parameters in session attributes map
            updateExtraParameters(customScriptConfiguration, this.authStep, sessionIdAttributes);

            if (sessionState != null) {
            	boolean updateResult = updateSession(sessionState, sessionIdAttributes);
            	if (!updateResult) {
            		return Constants.RESULT_FAILURE;
            	}
            }

            return Constants.RESULT_SUCCESS;
        } else {
            return Constants.RESULT_FAILURE;
        }
    }

    public boolean authenticateBySessionState(String p_sessionState) {
        if (StringUtils.isNotBlank(p_sessionState) && appConfiguration.getSessionIdEnabled()) {
            try {
                SessionState sessionState = sessionStateService.getSessionState(p_sessionState);
                return authenticateBySessionState(sessionState);
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
            }
        }

        return false;
    }

    public boolean authenticateBySessionState(SessionState sessionState) {
        if (sessionState == null) {
            return false;
        }
        String p_sessionState = sessionState.getId();

        log.trace("authenticateBySessionState, sessionState = '{}', session = '{}', state= '{}'", p_sessionState, sessionState, sessionState.getState());
        // IMPORTANT : authenticate by session state only if state of session is authenticated!
        if (SessionIdState.AUTHENTICATED == sessionState.getState()) {
            final User user = authenticationService.getUserOrRemoveSession(sessionState);
            if (user != null) {
                try {
                    authenticationService.configureEventUser(sessionState);
                } catch (Exception e) {
                    log.trace(e.getMessage(), e);
                }

                return true;
            }
        }

        return false;
    }

    private void initCustomAuthenticatorVariables(Map<String, String> sessionIdAttributes) {
        if (sessionIdAttributes == null) {
            log.error("Failed to restore attributes from session attributes");
            return;
        }

        this.authStep = StringHelper.toInteger(sessionIdAttributes.get("auth_step"), null);
        this.authAcr = sessionIdAttributes.get(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
    }

    private boolean authenticationFailed() {
        if (!this.addedErrorMessage) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "login.errorMessage", "login.errorMessage"));
        }
        return false;
    }

    private void authenticationFailedSessionInvalid() {
        this.addedErrorMessage = true;
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "login.errorSessionInvalidMessage", "login.errorSessionInvalidMessage"));
        facesService.redirect("/error.xhtml");
    }

    private void markAuthStepAsPassed(Map<String, String> sessionIdAttributes, Integer authStep) {
        String key = String.format("auth_step_passed_%d", authStep);
        sessionIdAttributes.put(key, Boolean.TRUE.toString());
    }

    private boolean isAuthStepPassed(Map<String, String> sessionIdAttributes, Integer authStep) {
        String key = String.format("auth_step_passed_%d", authStep);
        if (sessionIdAttributes.containsKey(key) && Boolean.parseBoolean(sessionIdAttributes.get(key))) {
            return true;
        }

        return false;
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

    public void configureSessionClient(Client client) {
        authenticationService.configureSessionClient(client);
    }

}