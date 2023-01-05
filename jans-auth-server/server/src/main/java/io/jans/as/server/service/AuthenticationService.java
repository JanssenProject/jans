/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.model.common.SimpleUser;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.common.UserService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.jsf2.service.FacesService;
import io.jans.model.GluuStatus;
import io.jans.model.SimpleProperty;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.model.metric.MetricType;
import io.jans.model.security.Credentials;
import io.jans.model.security.SimplePrincipal;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomEntry;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.ArrayHelper;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Authentication service methods
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version November 23, 2017
 */
@RequestScoped
public class AuthenticationService {

    private static final String AUTH_EXTERNAL_ATTRIBUTES = "auth_external_attributes";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Identity identity;

    @Inject
    private Credentials credentials;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_AUTH_CONFIG_NAME)
    private List<GluuLdapConfiguration> ldapAuthConfigs;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME)
    private List<PersistenceEntryManager> ldapAuthEntryManagers;

    @Inject
    private UserService userService;

    @Inject
    private ClientService clientService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private MetricService metricService;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private FacesService facesService;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private AuthenticationProtectionService authenticationProtectionService;

    /**
     * Authenticate user.
     *
     * @param userName The username.
     * @param password The user's password.
     * @return <code>true</code> if success, otherwise <code>false</code>.
     */
    public boolean authenticate(String userName, String password) {
        log.debug("Authenticating user with LDAP: username: '{}', credentials: '{}'", userName,
                System.identityHashCode(credentials));

        boolean authenticated = false;
        boolean protectionServiceEnabled = authenticationProtectionService.isEnabled();

        com.codahale.metrics.Timer.Context timerContext = null;
        timerContext = metricService
                .getTimer(MetricType.USER_AUTHENTICATION_RATE).time();
        try {
            if ((this.ldapAuthConfigs == null) || (this.ldapAuthConfigs.size() == 0)) {
                authenticated = localAuthenticate(userName, password);
            } else {
                authenticated = externalAuthenticate(userName, password);
            }
        } finally {
            timerContext.stop();
        }

        String userId = userName;
        if ((identity.getUser() != null) && StringHelper.isNotEmpty(identity.getUser().getUserId())) {
            userId = identity.getUser().getUserId();
        }
        setAuthenticatedUserSessionAttribute(userId, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        if (protectionServiceEnabled) {
            authenticationProtectionService.storeAttempt(userId, authenticated);
            authenticationProtectionService.doDelayIfNeeded(userId);
        }

        return authenticated;
    }

    /**
     * Authenticate user.
     *
     * @param nameValue      The name value to find user
     * @param password       The user's password.
     * @param nameAttributes List of attribute to search.
     * @return <code>true</code> if success, otherwise <code>false</code>.
     */
    public boolean authenticate(String nameValue, String password, String... nameAttributes) {
        log.debug("Authenticating user with LDAP: nameValue: '{}', nameAttributes: '{}', credentials: '{}'", nameValue,
                ArrayHelper.toString(nameAttributes),
                System.identityHashCode(credentials));

        Pair<Boolean, User> authenticatedPair = null;
        boolean authenticated = false;
        boolean protectionServiceEnabled = authenticationProtectionService.isEnabled();

        com.codahale.metrics.Timer.Context timerContext = metricService
                .getTimer(MetricType.USER_AUTHENTICATION_RATE).time();
        try {
            authenticatedPair = localAuthenticate(nameValue, password, nameAttributes);
        } finally {
            timerContext.stop();
        }

        String userId = null;
        if ((authenticatedPair != null) && (authenticatedPair.getSecond() != null)) {
            authenticated = authenticatedPair.getFirst();
            userId = authenticatedPair.getSecond().getUserId();
        }
        setAuthenticatedUserSessionAttribute(userId, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        if (protectionServiceEnabled) {
            authenticationProtectionService.storeAttempt(userId, authenticated);
            authenticationProtectionService.doDelayIfNeeded(userId);
        }

        return authenticated;
    }

    private void setAuthenticatedUserSessionAttribute(String userName, boolean authenticated) {
        SessionId sessionId = sessionIdService.getSessionId();
        if (sessionId != null) {
            Map<String, String> sessionIdAttributes = sessionId.getSessionAttributes();
            if (authenticated) {
                sessionIdAttributes.put(Constants.AUTHENTICATED_USER, userName);
            }
            sessionIdService.updateSessionIdIfNeeded(sessionId, authenticated);
        }
    }

    private boolean localAuthenticate(String userName, String password) {
        User user = userService.getUser(userName);
        if (user != null) {
            if (!checkUserStatus(user)) {
                return false;
            }

            // Use local LDAP server for user authentication
            boolean authenticated = false;
            try {
                authenticated = ldapEntryManager.authenticate(user.getDn(), User.class, password);
            } catch (AuthenticationException ex) {
                log.error("Authentication failed: " + ex.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Authentication failed:", ex);
                }
            }
            if (authenticated) {
                configureAuthenticatedUser(user);
                updateLastLogonUserTime(user);

                log.trace("Authenticate: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'",
                        System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());
            }

            return authenticated;
        }

        return false;
    }

    private Pair<Boolean, User> localAuthenticate(String nameValue, String password, String... nameAttributes) {
        String lowerNameValue = StringHelper.toString(nameValue);
        User user = userService.getUserByAttributes(lowerNameValue, nameAttributes, "uid", "jansStatus");
        if (user != null) {
            if (!checkUserStatus(user)) {
                return new Pair<Boolean, User>(false, user);
            }

            // Use local LDAP server for user authentication
            boolean authenticated = ldapEntryManager.authenticate(user.getDn(), password);
            if (authenticated) {
                configureAuthenticatedUser(user);
                updateLastLogonUserTime(user);

                log.trace("Authenticate: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'",
                        System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());
            }

            return new Pair<Boolean, User>(authenticated, user);
        }

        return new Pair<Boolean, User>(false, null);
    }

    private boolean externalAuthenticate(String keyValue, String password) {
        for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
            GluuLdapConfiguration ldapAuthConfig = this.ldapAuthConfigs.get(i);
            PersistenceEntryManager ldapAuthEntryManager = this.ldapAuthEntryManagers.get(i);

            String primaryKey = "uid";
            if (StringHelper.isNotEmpty(ldapAuthConfig.getPrimaryKey())) {
                primaryKey = ldapAuthConfig.getPrimaryKey();
            }

            String localPrimaryKey = "uid";
            if (StringHelper.isNotEmpty(ldapAuthConfig.getLocalPrimaryKey())) {
                localPrimaryKey = ldapAuthConfig.getLocalPrimaryKey();
            }

            boolean authenticated = authenticate(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey,
                    localPrimaryKey, false);
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
        boolean protectionServiceEnabled = authenticationProtectionService.isEnabled();

        com.codahale.metrics.Timer.Context timerContext = metricService
                .getTimer(MetricType.USER_AUTHENTICATION_RATE).time();
        try {
            for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
                GluuLdapConfiguration ldapAuthConfig = this.ldapAuthConfigs.get(i);
                PersistenceEntryManager ldapAuthEntryManager = this.ldapAuthEntryManagers.get(i);

                authenticated = authenticate(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey,
                        localPrimaryKey, false);
                if (authenticated) {
                    break;
                }
            }
        } finally {
            timerContext.stop();
        }
        String userId = null;
        if ((identity.getUser() != null) && StringHelper.isNotEmpty(identity.getUser().getUserId())) {
            userId = identity.getUser().getUserId();
        }
        setAuthenticatedUserSessionAttribute(userId, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        if (protectionServiceEnabled) {
            authenticationProtectionService.storeAttempt(keyValue, authenticated);
            authenticationProtectionService.doDelayIfNeeded(keyValue);
        }

        return authenticated;
    }

    /*
     * Utility method which can be used in custom scripts
     */
    public boolean authenticate(GluuLdapConfiguration ldapAuthConfig, PersistenceEntryManager ldapAuthEntryManager,
                                String keyValue, String password, String primaryKey, String localPrimaryKey) {

        return authenticate(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey, localPrimaryKey, true);
    }

    /*
     * Utility method which can be used in custom scripts
     */
    public boolean authenticate(GluuLdapConfiguration ldapAuthConfig, PersistenceEntryManager ldapAuthEntryManager,
                                String keyValue, String password, String primaryKey, String localPrimaryKey, boolean updateMetrics) {
        boolean authenticated = false;
        boolean protectionServiceEnabled = authenticationProtectionService.isEnabled();

        com.codahale.metrics.Timer.Context timerContext = null;

        if (updateMetrics) {
            timerContext = metricService.getTimer(MetricType.USER_AUTHENTICATION_RATE).time();
        }

        try {
            authenticated = authenticateImpl(ldapAuthConfig, ldapAuthEntryManager, keyValue, password, primaryKey, localPrimaryKey);
        } finally {
            if (updateMetrics) {
                timerContext.stop();
            }
        }

        String userId = keyValue;
        if ((identity.getUser() != null) && StringHelper.isNotEmpty(identity.getUser().getUserId())) {
            userId = identity.getUser().getUserId();
        }
        setAuthenticatedUserSessionAttribute(userId, authenticated);

        if (updateMetrics) {
            MetricType metricType;
            if (authenticated) {
                metricType = MetricType.USER_AUTHENTICATION_SUCCESS;
            } else {
                metricType = MetricType.USER_AUTHENTICATION_FAILURES;
            }

            metricService.incCounter(metricType);
        }

        if (protectionServiceEnabled) {
            authenticationProtectionService.storeAttempt(userId, authenticated);
            authenticationProtectionService.doDelayIfNeeded(userId);
        }

        return authenticated;
    }

    private boolean authenticateImpl(GluuLdapConfiguration ldapAuthConfig, PersistenceEntryManager ldapAuthEntryManager,
                                     String keyValue, String password, String primaryKey, String localPrimaryKey) {
        log.debug("Attempting to find userDN by primary key: '{}' and key value: '{}', credentials: '{}'", primaryKey,
                keyValue, System.identityHashCode(credentials));

        try {
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

                                log.trace(
                                        "authenticate_external: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'",
                                        System.identityHashCode(credentials), credentials.getUsername(),
                                        getAuthenticatedUserId());

                                return true;
                            }
                        }
                    }
                }
            } else {
                log.error("There are no baseDns specified in authentication configuration.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public boolean authenticate(String userName) {
        log.debug("Authenticating user with LDAP: username: '{}', credentials: '{}'", userName,
                System.identityHashCode(credentials));

        boolean authenticated = false;
        boolean protectionServiceEnabled = authenticationProtectionService.isEnabled();

        com.codahale.metrics.Timer.Context timerContext = metricService
                .getTimer(MetricType.USER_AUTHENTICATION_RATE).time();
        try {
            User user = userService.getUser(userName);
            if ((user != null) && checkUserStatus(user)) {
                credentials.setUsername(user.getUserId());
                configureAuthenticatedUser(user);
                updateLastLogonUserTime(user);

                log.trace("Authenticate: credentials: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'",
                        System.identityHashCode(credentials), credentials.getUsername(), getAuthenticatedUserId());

                authenticated = true;
            }
        } finally {
            timerContext.stop();
        }

        setAuthenticatedUserSessionAttribute(userName, authenticated);

        MetricType metricType;
        if (authenticated) {
            metricType = MetricType.USER_AUTHENTICATION_SUCCESS;
        } else {
            metricType = MetricType.USER_AUTHENTICATION_FAILURES;
        }

        metricService.incCounter(metricType);

        if (protectionServiceEnabled) {
            authenticationProtectionService.storeAttempt(userName, authenticated);
            authenticationProtectionService.doDelayIfNeeded(userName);
        }

        return authenticated;
    }

    private User getUserByAttribute(PersistenceEntryManager ldapAuthEntryManager, String baseDn, String attributeName,
                                    String attributeValue) {
        log.debug("Getting user information from LDAP: attributeName = '{}', attributeValue = '{}'", attributeName,
                attributeValue);

        if (StringHelper.isEmpty(attributeValue)) {
            return null;
        }

        SimpleUser sampleUser = new SimpleUser();
        sampleUser.setDn(baseDn);

        List<CustomObjectAttribute> customAttributes = new ArrayList<CustomObjectAttribute>();
        customAttributes.add(new CustomObjectAttribute(attributeName, attributeValue));

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
        CustomObjectAttribute userStatus = userService.getCustomAttribute(user, "jansStatus");

        if ((userStatus != null) && GluuStatus.ACTIVE.getValue().equalsIgnoreCase(StringHelper.toString(userStatus.getValue()))) {
            return true;
        }

        log.warn("User '{}' is disabled", user.getUserId());
        return false;
    }

    private void updateLastLogonUserTime(User user) {
        if (!appConfiguration.getUpdateUserLastLogonTime()) {
            return;
        }

        CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(user.getDn());

        List<String> personCustomObjectClassList = userService.getPersonCustomObjectClassList();
        if ((personCustomObjectClassList != null) && !personCustomObjectClassList.isEmpty()) {
            // Combine object classes from LDAP and configuration in one list
            Set<Object> customPersonCustomObjectClassList = new HashSet<Object>();
            customPersonCustomObjectClassList.add(AttributeConstants.JANS_PERSON);
            customPersonCustomObjectClassList.addAll(personCustomObjectClassList);
            if (user.getCustomObjectClasses() != null) {
                customPersonCustomObjectClassList.addAll(Arrays.asList(user.getCustomObjectClasses()));
            }

            customEntry.setCustomObjectClasses(
                    customPersonCustomObjectClassList.toArray(new String[customPersonCustomObjectClassList.size()]));
        } else {
            customEntry.setCustomObjectClasses(UserService.USER_OBJECT_CLASSES);
        }

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        String nowDateString = ldapEntryManager.encodeTime(customEntry.getDn(), now);
        CustomAttribute customAttribute = new CustomAttribute("jansLastLogonTime", nowDateString);
        customEntry.getCustomAttributes().add(customAttribute);

        try {
            ldapEntryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update jansLastLogonTime of user '{}'", user.getUserId());
            log.trace("Failed to update user:", epe);
        }
    }

    public SessionId configureSessionUser(SessionId sessionId, Map<String, String> sessionIdAttributes) {
        log.trace("configureSessionUser: credentials: '{}', sessionId: '{}', credentials.userName: '{}', authenticatedUser.userId: '{}'",
                System.identityHashCode(credentials), sessionId, credentials.getUsername(), getAuthenticatedUserId());

        User user = getAuthenticatedUser();

        String sessionAuthUser = sessionIdAttributes.get(Constants.AUTHENTICATED_USER);
        log.trace("configureSessionUser sessionId: '{}', sessionId.auth_user: '{}'", sessionId, sessionAuthUser);

        SessionId newSessionId = sessionIdService.setSessionIdStateAuthenticated(getHttpRequest(), getHttpResponse(), sessionId, user.getDn());

        identity.setSessionId(sessionId);
        newSessionId.setUser(user);

        return newSessionId;
    }

    public SessionId configureEventUser() {
        User user = getAuthenticatedUser();
        if (user == null) {
            return null;
        }

        log.debug("ConfigureEventUser: username: '{}', credentials: '{}'", user.getUserId(),
                System.identityHashCode(credentials));

        SessionId sessionId = sessionIdService.generateAuthenticatedSessionId(getHttpRequest(), user.getDn());

        identity.setSessionId(sessionId);

        return sessionId;
    }

    private HttpServletRequest getHttpRequest() {
        if (externalContext == null) {
            return null;
        }
        return (HttpServletRequest) externalContext.getRequest();
    }

    private HttpServletResponse getHttpResponse() {
        if (externalContext == null) {
            return null;
        }
        return (HttpServletResponse) externalContext.getResponse();
    }

    public void configureEventUser(SessionId sessionId) {
        sessionIdService.updateSessionId(sessionId);

        identity.setSessionId(sessionId);
    }

    public void quietLogin(String userName) {
        Principal principal = new SimplePrincipal(userName);
        identity.acceptExternallyAuthenticatedPrincipal(principal);
        identity.quietLogin();
    }

    private void configureAuthenticatedUser(User user) {
        identity.setUser(user);
    }

    public User getAuthenticatedUser() {
        if (identity.getUser() != null) {
            return identity.getUser();
        } else {
            SessionId sessionId = sessionIdService.getSessionId();
            if (sessionId != null) {
                Map<String, String> sessionIdAttributes = sessionId.getSessionAttributes();
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

    public String getAuthenticatedUserId() {
        User authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser != null) {
            return authenticatedUser.getUserId();
        }

        return null;
    }

    public Client configureSessionClient() {
        String clientInum = credentials.getUsername();
        log.debug("ConfigureSessionClient: username: '{}', credentials: '{}'", clientInum,
                System.identityHashCode(credentials));

        Client client = clientService.getClient(clientInum);
        configureSessionClient(client);
        return client;
    }

    public void configureSessionClient(Client client) {
        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        identity.setSessionClient(sessionClient);

        clientService.updateAccessTime(client, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onSuccessfulLogin(SessionId sessionUser) {
        log.info("Attempting to redirect user: SessionUser: {}", sessionUser != null ? sessionUser.getId() : null);

        if ((sessionUser == null) || StringUtils.isBlank(sessionUser.getUserDn())) {
            return;
        }

        User user = sessionIdService.getUser(sessionUser);

        log.info("Attempting to redirect user: User: {}", user);
        if (user == null) {
            log.error("Failed to identify logged in user for session: {}", sessionUser);
            return;
        }

        final Map<String, String> result = sessionUser.getSessionAttributes();
        result.put(AuthorizeResponseParam.SESSION_ID, sessionUser.getId()); // parameters must be filled before filtering
        result.put(AuthorizeResponseParam.SID, sessionUser.getOutsideSid()); // parameters must be filled before filtering

        Map<String, String> allowedParameters = requestParameterService.getAllowedParameters(result);

        log.trace("Logged in successfully! User: {}, page: /authorize.xhtml, map: {}", user, allowedParameters);
        facesService.redirect("/authorize.xhtml", (Map) allowedParameters);
    }

    public User getUserOrRemoveSession(SessionId p_sessionId) {
        if (p_sessionId != null) {
            try {
                if (StringUtils.isNotBlank(p_sessionId.getUserDn())) {
                    final User user = sessionIdService.getUser(p_sessionId);
                    if (user != null) {
                        return user;
                    } else { // if there is no user than session is invalid
                        sessionIdService.remove(p_sessionId);
                    }
                } else { // if there is no user than session is invalid
                    sessionIdService.remove(p_sessionId);
                }
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
            }
        }
        return null;
    }

    public String parametersAsString() throws UnsupportedEncodingException {
        final Map<String, String> parameterMap = getParametersMap(null);

        return requestParameterService.parametersAsString(parameterMap);
    }

    public Map<String, String> getParametersMap(List<String> extraParameters) {
        final Map<String, String> parameterMap = new HashMap<String, String>(externalContext.getRequestParameterMap());

        return requestParameterService.getParametersMap(extraParameters, parameterMap);
    }

    public boolean isParameterExists(String p_name) {
        return identity.isSetWorkingParameter(p_name);
    }

    public void updateExtraParameters(Map<String, String> sessionIdAttributes, List<String> extraParameters) {
        // Load extra parameters set
        Map<String, String> authExternalAttributes = getExternalScriptExtraParameters(sessionIdAttributes);

        if (extraParameters != null) {
            log.trace("Attempting to store extraParameters: {}", extraParameters);
            for (String extraParameter : extraParameters) {
                if (isParameterExists(extraParameter)) {
                    Pair<String, String> extraParameterValueWithType = requestParameterService
                            .getParameterValueWithType(extraParameter);
                    String extraParameterValue = extraParameterValueWithType.getFirst();
                    String extraParameterType = extraParameterValueWithType.getSecond();

                    // Store parameter name and value
                    sessionIdAttributes.put(extraParameter, extraParameterValue);

                    // Store parameter name and type
                    authExternalAttributes.put(extraParameter, extraParameterType);
                }
            }
        }

        // Store identity working parameters in session
        setExternalScriptExtraParameters(sessionIdAttributes, authExternalAttributes);
        log.trace("Storing sessionIdAttributes: {}", sessionIdAttributes);
        log.trace("Storing authExternalAttributes: {}", authExternalAttributes);
    }

    public Map<String, String> getExternalScriptExtraParameters(Map<String, String> sessionIdAttributes) {
        String authExternalAttributesString = sessionIdAttributes.get(AUTH_EXTERNAL_ATTRIBUTES);
        Map<String, String> authExternalAttributes = new HashMap<String, String>();
        try {
            authExternalAttributes = Util.jsonObjectArrayStringAsMap(authExternalAttributesString);
        } catch (JSONException ex) {
            log.error("Failed to convert JSON array of auth_external_attributes to Map<String, String>");
        }

        return authExternalAttributes;
    }

    public void setExternalScriptExtraParameters(Map<String, String> sessionIdAttributes,
                                                 Map<String, String> authExternalAttributes) {
        String authExternalAttributesString = null;
        try {
            authExternalAttributesString = Util.mapAsString(authExternalAttributes);
        } catch (JSONException ex) {
            log.error("Failed to convert Map<String, String> of auth_external_attributes to JSON array");
        }

        sessionIdAttributes.put(AUTH_EXTERNAL_ATTRIBUTES, authExternalAttributesString);
    }

    public void clearExternalScriptExtraParameters(Map<String, String> sessionIdAttributes) {
        Map<String, String> authExternalAttributes = getExternalScriptExtraParameters(sessionIdAttributes);

        for (String authExternalAttribute : authExternalAttributes.keySet()) {
            sessionIdAttributes.remove(authExternalAttribute);
        }

        sessionIdAttributes.remove(AUTH_EXTERNAL_ATTRIBUTES);
    }

    public void copyAuthenticatorExternalAttributes(SessionId oldSession, SessionId newSession) {
        if ((oldSession != null) && (oldSession.getSessionAttributes() != null) &&
                (newSession != null) && (newSession.getSessionAttributes() != null)) {

            Map<String, String> newSessionIdAttributes = newSession.getSessionAttributes();
            Map<String, String> oldSessionIdAttributes = oldSession.getSessionAttributes();

            Map<String, String> authExternalAttributes = getExternalScriptExtraParameters(oldSession.getSessionAttributes());

            if (authExternalAttributes != null) {
                log.trace("Attempting to copy extraParameters into new session: {}", authExternalAttributes);
                for (String authExternalAttributeName : authExternalAttributes.keySet()) {
                    if (oldSessionIdAttributes.containsKey(authExternalAttributeName)) {
                        String authExternalAttributeValue = oldSessionIdAttributes.get(authExternalAttributeName);

                        // Store in new session
                        newSessionIdAttributes.put(authExternalAttributeName, authExternalAttributeValue);
                    }
                }
            }

            setExternalScriptExtraParameters(newSessionIdAttributes, authExternalAttributes);
        }
    }

}