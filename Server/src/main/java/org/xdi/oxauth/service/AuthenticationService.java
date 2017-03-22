/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import static org.xdi.oxauth.model.authorize.AuthorizeResponseParam.SESSION_STATE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.service.FacesService;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.slf4j.Logger;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.ldap.model.GluuStatus;
import org.xdi.model.SimpleProperty;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.model.metric.MetricType;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.SimpleUser;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.SessionClient;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.security.Credentials;
import org.xdi.oxauth.security.Identity;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

/**
 * Authentication service methods
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
// TODO: Review CDI injection Identity into stateless
@Stateless
@Named
public class AuthenticationService {

	// use only "acr" instead of "acr_values" #334
    public static final List<String> ALLOWED_PARAMETER = Collections.unmodifiableList(Arrays.asList(
            AuthorizeRequestParam.SCOPE,
            AuthorizeRequestParam.RESPONSE_TYPE,
            AuthorizeRequestParam.CLIENT_ID,
            AuthorizeRequestParam.REDIRECT_URI,
            AuthorizeRequestParam.STATE,
            AuthorizeRequestParam.RESPONSE_MODE,
            AuthorizeRequestParam.NONCE,
            AuthorizeRequestParam.DISPLAY,
            AuthorizeRequestParam.PROMPT,
            AuthorizeRequestParam.MAX_AGE,
            AuthorizeRequestParam.UI_LOCALES,
            AuthorizeRequestParam.ID_TOKEN_HINT,
            AuthorizeRequestParam.LOGIN_HINT,
            AuthorizeRequestParam.ACR_VALUES,
            AuthorizeRequestParam.SESSION_STATE,
            AuthorizeRequestParam.REQUEST,
            AuthorizeRequestParam.REQUEST_URI,
            AuthorizeRequestParam.ORIGIN_HEADERS,
            AuthorizeRequestParam.CODE_CHALLENGE,
            AuthorizeRequestParam.CODE_CHALLENGE_METHOD,
            AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS));
    private static final String EVENT_CONTEXT_AUTHENTICATED_USER = "authenticatedUser";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Identity identity;
    
    @Inject
    private Credentials credentials;

    @Inject @Named(AppInitializer.LDAP_AUTH_CONFIG_NAME)
    private List<GluuLdapConfiguration> ldapAuthConfigs;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject @Named(AppInitializer.LDAP_AUTH_ENTRY_MANAGER_NAME)
    private List<LdapEntryManager> ldapAuthEntryManagers;

    @Inject
    private UserService userService;

    @Inject
    private ClientService clientService;

    @Inject
    private SessionStateService sessionStateService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private MetricService metricService;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private FacesService facesService;

    /**
     * Authenticate user.
     *
     * @param userName The username.
     * @param password The user's password.
     * @return <code>true</code> if success, otherwise <code>false</code>.
     */
    public boolean authenticate(String userName, String password) {
        log.debug("Authenticating user with LDAP: username: '{}', credentials: '{}'", userName, System.identityHashCode(credentials));

        boolean authenticated = false;

        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.OXAUTH_USER_AUTHENTICATION_RATE).time();
        try {
            if ((this.ldapAuthConfigs == null) || (this.ldapAuthConfigs.size() == 0)) {
                authenticated = localAuthenticate(userName, password);
            } else {
                authenticated = externalAuthenticate(userName, password);
            }
        } finally {
            timerContext.stop();
        }

        setAuthenticatedUserSessionAttribute(userName, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        return authenticated;
    }

    private void setAuthenticatedUserSessionAttribute(String userName, boolean authenticated) {
        SessionState sessionState = sessionStateService.getSessionState();
        if (sessionState != null) {
            Map<String, String> sessionIdAttributes = sessionState.getSessionAttributes();
            if (authenticated) {
                sessionIdAttributes.put(Constants.AUTHENTICATED_USER, userName);
            }
            sessionStateService.updateSessionStateIfNeeded(sessionState, authenticated);
        }
    }

    private boolean localAuthenticate(String userName, String password) {
        User user = userService.getUser(userName);
        if (user != null) {
            if (!checkUserStatus(user)) {
                return false;
            }

            // Use local LDAP server for user authentication
            boolean authenticated = ldapEntryManager.authenticate(user.getDn(), password);
            if (authenticated) {
                configureAuthenticatedUser(user);
                updateLastLogonUserTime(user);

                log.trace("Authenticate: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'", System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());
            }

            return authenticated;
        }

        return false;
    }

    private boolean externalAuthenticate(String keyValue, String password) {
        for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
            GluuLdapConfiguration ldapAuthConfig = this.ldapAuthConfigs.get(i);
            LdapEntryManager ldapAuthEntryManager = this.ldapAuthEntryManagers.get(i);

            String primaryKey = "uid";
            if (StringHelper.isNotEmpty(ldapAuthConfig.getPrimaryKey())) {
                primaryKey = ldapAuthConfig.getPrimaryKey();
            }

            String localPrimaryKey = "uid";
            if (StringHelper.isNotEmpty(ldapAuthConfig.getLocalPrimaryKey())) {
                localPrimaryKey = ldapAuthConfig.getLocalPrimaryKey();
            }

            boolean authenticated = authenticate(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey, localPrimaryKey);
            if (authenticated) {
                return authenticated;
            }
        }

        return false;
    }

    public boolean authenticate(String keyValue, String password, String primaryKey, String localPrimaryKey) {
        if (this.ldapAuthConfigs == null) {
            return authenticate(null, ldapEntryManager, keyValue, password, primaryKey, localPrimaryKey);
        }

        boolean authenticated = false;

        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.OXAUTH_USER_AUTHENTICATION_RATE).time();
        try {
            for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
                GluuLdapConfiguration ldapAuthConfig = this.ldapAuthConfigs.get(i);
                LdapEntryManager ldapAuthEntryManager = this.ldapAuthEntryManagers.get(i);

                authenticated = authenticate(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey, localPrimaryKey);
                if (authenticated) {
                    break;
                }
            }
        } finally {
            timerContext.stop();
        }

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        return authenticated;
    }

    /*
     * Utility method which can be used in custom scripts
     */
    public boolean authenticate(GluuLdapConfiguration ldapAuthConfig, LdapEntryManager ldapAuthEntryManager, String keyValue, String password, String primaryKey, String localPrimaryKey) {
        log.debug("Attempting to find userDN by primary key: '{}' and key value: '{}', credentials: '{}'", primaryKey, keyValue, System.identityHashCode(credentials));

        List<?> baseDNs;
        if (ldapAuthConfig == null) {
            baseDNs = Arrays.asList(userService.getDnForUser(null));
        } else {
            baseDNs = ldapAuthConfig.getBaseDNs();
        }

        if (baseDNs != null && !baseDNs.isEmpty()) {
            for (Object baseDnProperty : baseDNs) {
                String baseDn;
                if (baseDnProperty instanceof SimpleProperty) {
                    baseDn = ((SimpleProperty) baseDnProperty).getValue();
                } else {
                    baseDn = baseDnProperty.toString();
                }

                User user = getUserByAttribute(ldapAuthEntryManager, baseDn, primaryKey, keyValue);
                if (user != null) {
                    String userDn = user.getDn();
                    log.debug("Attempting to authenticate userDN: {}", userDn);
                    if (ldapAuthEntryManager.authenticate(userDn, password)) {
                        log.debug("User authenticated: {}", userDn);

                        log.debug("Attempting to find userDN by local primary key: {}", localPrimaryKey);
                        User localUser = userService.getUserByAttribute(localPrimaryKey, keyValue);
                        if (localUser != null) {
                            if (!checkUserStatus(localUser)) {
                                return false;
                            }

                            configureAuthenticatedUser(localUser);
                            updateLastLogonUserTime(localUser);

                            log.trace("authenticate_external: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'", System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());

                            return true;
                        }
                    }
                }
            }
        } else {
            log.error("There are no baseDns specified in authentication configuration.");
        }

        return false;
    }

    public boolean authenticate(String userName) {
        Credentials credentials = ServerUtil.bean(Credentials.class);
        log.debug("Authenticating user with LDAP: username: '{}', credentials: '{}'", userName, System.identityHashCode(credentials));

        boolean authenticated = false;

        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.OXAUTH_USER_AUTHENTICATION_RATE).time();
        try {
            User user = userService.getUser(userName);
            if ((user != null) && checkUserStatus(user)) {
                credentials.setUsername(user.getUserId());
                configureAuthenticatedUser(user);
                updateLastLogonUserTime(user);

                log.trace("Authenticate: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'", System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());

                authenticated = true;
            }
        } finally {
            timerContext.stop();
        }

        setAuthenticatedUserSessionAttribute(userName, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.OXAUTH_USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        return authenticated;
    }

    private User getUserByAttribute(LdapEntryManager ldapAuthEntryManager, String baseDn, String attributeName, String attributeValue) {
        log.debug("Getting user information from LDAP: attributeName = '{}', attributeValue = '{}'", attributeName, attributeValue);

        if (StringHelper.isEmpty(attributeValue)) {
            return null;
        }

        SimpleUser sampleUser = new SimpleUser();
        sampleUser.setDn(baseDn);

        List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();
        customAttributes.add(new CustomAttribute(attributeName, attributeValue));

        sampleUser.setCustomAttributes(customAttributes);

        log.debug("Searching user by attributes: '{}', baseDn: '{}'", customAttributes, baseDn);
        List<User> entries = ldapAuthEntryManager.findEntries(sampleUser, 1);
        log.debug("Found '{}' entries", entries.size());

        if (entries.size() > 0) {
            SimpleUser foundUser = entries.get(0);

            return ldapAuthEntryManager.find(User.class, foundUser.getDn());
        } else {
            return null;
        }
    }

    private boolean checkUserStatus(User user) {
        CustomAttribute userStatus = userService.getCustomAttribute(user, "gluuStatus");

        if ((userStatus != null) && GluuStatus.ACTIVE.equals(GluuStatus.getByValue(userStatus.getValue()))) {
            return true;
        }

        log.warn("User '{}' was disabled", user.getUserId());
        return false;
    }

    private void updateLastLogonUserTime(User user) {
		if (!appConfiguration.getUpdateUserLastLogonTime()) {
			return;
		}

		CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(user.getDn());
		customEntry.setCustomObjectClasses(UserService.USER_OBJECT_CLASSES);

        CustomAttribute customAttribute = new CustomAttribute("oxLastLogonTime", new Date());
        customEntry.getCustomAttributes().add(customAttribute);

        try {
            ldapEntryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update oxLastLoginTime of user '{}'", user.getUserId());
        }
    }

    public SessionState configureSessionUser(SessionState sessionState, Map<String, String> sessionIdAttributes) {
        Credentials credentials = ServerUtil.bean(Credentials.class);

        log.trace("configureSessionUser: credentials: '{}', sessionState: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'", System.identityHashCode(credentials), sessionState, credentials.getUsername(), getAuthenticatedUserId());

        User user = getAuthenticatedUser();

        SessionState newSessionState;
        if (sessionState == null) {
            newSessionState = sessionStateService.generateAuthenticatedSessionState(user.getDn(), sessionIdAttributes);
        } else {
            // TODO: Remove after 2.4.5
            String sessionAuthUser = sessionIdAttributes.get(Constants.AUTHENTICATED_USER);
            log.trace("configureSessionUser sessionState: '{}', sessionState.auth_user: '{}'", sessionState, sessionAuthUser);

            newSessionState = sessionStateService.setSessionStateAuthenticated(sessionState, user.getDn());
        }

        configureEventUserContext(newSessionState);

        return newSessionState;
    }

    public SessionState configureEventUser() {
        Credentials credentials = ServerUtil.bean(Credentials.class);

        User user = getAuthenticatedUser();
        if (user == null) {
            return null;
        }

        log.debug("ConfigureEventUser: username: '{}', credentials: '{}'", user.getUserId(), System.identityHashCode(credentials));

        SessionState sessionState = sessionStateService.generateAuthenticatedSessionState(user.getDn());

        configureEventUserContext(sessionState);

        return sessionState;
    }

    public void configureEventUser(SessionState sessionState) {
        sessionStateService.updateSessionState(sessionState);

        configureEventUserContext(sessionState);
    }

    private void configureEventUserContext(SessionState sessionState) {
        identity.setSessionState(sessionState);
    }

    private void configureAuthenticatedUser(User user) {
        identity.setUser(user);
    }

    public User getAuthenticatedUser() {
        if (identity.getUser() != null) {
            return identity.getUser();
        } else {
            SessionState sessionState = sessionStateService.getSessionState();
            if (sessionState != null) {
                Map<String, String> sessionIdAttributes = sessionState.getSessionAttributes();
                String userId = sessionIdAttributes.get(Constants.AUTHENTICATED_USER);
                if (StringHelper.isNotEmpty(userId)) {
                    User user = userService.getUser(userId);
                    identity.setUser(user);

                    return user;
                }
            }
        }

        return null;
    }

    private String getAuthenticatedUserId() {
        User authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser != null) {
            return authenticatedUser.getUserId();
        }

        return null;
    }

    public void configureSessionClient() {
        Credentials credentials = ServerUtil.bean(Credentials.class);
        String clientInum = credentials.getUsername();
        log.debug("ConfigureSessionClient: username: '{}', credentials: '{}'", clientInum, System.identityHashCode(credentials));

        Client client = clientService.getClient(clientInum);
        configureSessionClient(client);
    }

    public void configureSessionClient(Client client) {
        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        identity.setSessionClient(sessionClient);

        clientService.updatAccessTime(client, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
//    @Observer(value = {Constants.EVENT_OXAUTH_CUSTOM_LOGIN_SUCCESSFUL, Identity.EVENT_LOGIN_SUCCESSFUL})
    public void onSuccessfulLogin(SessionState sessionUser) {
        log.info("Attempting to redirect user: SessionUser: {}", sessionUser);

        if ((sessionUser == null) || StringUtils.isBlank(sessionUser.getUserDn())) {
            return;
        }

        User user = userService.getUserByDn(sessionUser.getUserDn());

        log.info("Attempting to redirect user: User: {}", user);

        if (user != null) {
            final Map<String, String> result = sessionUser.getSessionAttributes();
            Map<String, String> allowedParameters = getAllowedParameters(result);

            result.put(SESSION_STATE, sessionUser.getId());

            log.trace("Logged in successfully! User: {}, page: /authorize.xhtml, map: {}", user, allowedParameters);
            facesService.redirect("/authorize.xhtml", (Map) allowedParameters);
        }
    }

    public static Map<String, String> getAllowedParameters(@Nonnull final Map<String, String> requestParameterMap) {
        final Map<String, String> result = new HashMap<String, String>();
        if (!requestParameterMap.isEmpty()) {
            final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
            for (Map.Entry<String, String> entry : set) {
                if (ALLOWED_PARAMETER.contains(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    public User getUserOrRemoveSession(SessionState p_sessionState) {
        if (p_sessionState != null) {
            try {
                if (StringUtils.isNotBlank(p_sessionState.getUserDn())) {
                    final User user = userService.getUserByDn(p_sessionState.getUserDn());
                    if (user != null) {
                        return user;
                    } else { // if there is no user than session is invalid
                        sessionStateService.remove(p_sessionState);
                    }
                } else { // if there is no user than session is invalid
                    sessionStateService.remove(p_sessionState);
                }
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
            }
        }
        return null;
    }

    public String parametersAsString() throws UnsupportedEncodingException {
        final Map<String, String> parameterMap = getParametersMap(null);

        return parametersAsString(parameterMap);
    }

    public String parametersAsString(final Map<String, String> parameterMap) throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder();
        final Set<Entry<String, String>> set = parameterMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            final String value = (String) entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(value, Util.UTF8_STRING_ENCODING)).append("&");
            }
        }

        String result = sb.toString();
        if (result.endsWith("&")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public Map<String, String> getParametersMap(List<String> extraParameters) {
        final Map<String, String> parameterMap = new HashMap<String, String>(externalContext
                .getRequestParameterMap());

        return getParametersMap(extraParameters, parameterMap);
    }

    public Map<String, String> getParametersMap(List<String> extraParameters, final Map<String, String> parameterMap) {
        final List<String> allowedParameters = new ArrayList<String>(ALLOWED_PARAMETER);

        if (extraParameters != null) {
            for (String extraParameter : extraParameters) {
                putInMap(parameterMap, extraParameter);
            }

            allowedParameters.addAll(extraParameters);
        }

        for (Iterator<Entry<String, String>> it = parameterMap.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, String> entry = it.next();
            if (!allowedParameters.contains(entry.getKey())) {
                it.remove();
            }
        }

        return parameterMap;
    }

    private void putInMap(Map<String, String> map, String p_name) {
        if (map == null) {
            return;
        }

        String value = getParameterValue(p_name);

        map.put(p_name, value);
    }

    public String getParameterValue(String p_name) {
        final Object o = identity.getWorkingParameter(p_name);
        if (o instanceof String) {
            final String s = (String) o;
            return s;
        } else if (o instanceof Integer) {
            final Integer i = (Integer) o;
            return i.toString();
        } else if (o instanceof Boolean) {
            final Boolean b = (Boolean) o;
            return b.toString();
        }

        return null;
    }

    public boolean isParameterExists(String p_name) {
        return identity.gisSetWorkingParameter(p_name);
    }

}