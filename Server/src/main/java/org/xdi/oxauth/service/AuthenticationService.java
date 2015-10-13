/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesManager;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.ldap.model.GluuStatus;
import org.xdi.model.SimpleProperty;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.model.metric.MetricType;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.CustomAttribute;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.common.SimpleUser;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.OAuthCredentials;
import org.xdi.oxauth.model.session.SessionClient;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

import javax.annotation.Nonnull;
import javax.faces.context.FacesContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

/**
 * Authentication service methods
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version October 1, 2015
 */
@Scope(ScopeType.STATELESS)
@Name("authenticationService")
@AutoCreate
public class AuthenticationService {

    public static final List<String> ALLOWED_PARAMETER = Collections.unmodifiableList(Arrays.asList(
            "scope", "response_type", "client_id", "redirect_uri", "state", "response_mode", "nonce", "display", "prompt", "max_age",
            "ui_locales", "id_token_hint", "login_hint", "acr_values", "session_id", "request", "request_uri",
            AuthorizeRequestParam.ORIGIN_HEADERS));

    @Logger
    private Log log;

    @In
    private Identity identity;

    @In
    private OAuthCredentials credentials;

    @In(required = false, value = AppInitializer.LDAP_AUTH_CONFIG_NAME)
    // created by app initializer
    private List<GluuLdapConfiguration> ldapAuthConfigs;

    @In
    private LdapEntryManager ldapEntryManager;

    @In(required = true, value = AppInitializer.LDAP_AUTH_ENTRY_MANAGER_NAME)
    private List<LdapEntryManager> ldapAuthEntryManagers;

    @In
    private UserService userService;

    @In
    private ClientService clientService;

    @In
    private SessionIdService sessionIdService;

    @In
    private ExternalAuthenticationService externalAuthenticationService;

    @In
    private SessionId sessionUser;

    @In
    private MetricService metricService;

    /**
     * Authenticate user.
     *
     * @param userName The username.
     * @param password The user's password.
     * @return <code>true</code> if success, otherwise <code>false</code>.
     */
    public boolean authenticate(String userName, String password) {
        log.debug("Authenticating user with LDAP: username: {0}", userName);

        boolean authenticated = false;

        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.OXAUTH_USER_AUTHENTICATION_RATE).time();
        try {
            if (this.ldapAuthConfigs == null) {
                authenticated = localAuthenticate(userName, password);
            } else {
                authenticated = externalAuthenticate(userName, password);
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

    private boolean localAuthenticate(String userName, String password) {
        User user = userService.getUser(userName);
        if (user != null) {
            if (!checkUserStatus(user)) {
                return false;
            }

            // Use local LDAP server for user authentication
            boolean authenticated = ldapEntryManager.authenticate(user.getDn(), password);
            if (authenticated) {
                credentials.setUser(user);
                updateLastLogonUserTime(user);
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

    private boolean authenticate(GluuLdapConfiguration ldapAuthConfig, LdapEntryManager ldapAuthEntryManager, String keyValue, String password, String primaryKey, String localPrimaryKey) {
        log.debug("Attempting to find userDN by primary key: '{0}' and key value: '{1}'", primaryKey, keyValue);

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
                    log.debug("Attempting to authenticate userDN: {0}", userDn);
                    if (ldapAuthEntryManager.authenticate(userDn, password)) {
                        log.debug("User authenticated: {0}", userDn);

                        log.debug("Attempting to find userDN by local primary key: {0}", localPrimaryKey);
                        User localUser = userService.getUserByAttribute(localPrimaryKey, keyValue);
                        if (localUser != null) {
                            if (!checkUserStatus(localUser)) {
                                return false;
                            }

                            credentials.setUser(localUser);
                            updateLastLogonUserTime(localUser);

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
        log.debug("Authenticating user with LDAP: username: {0}", userName);

        boolean authenticated = false;

        com.codahale.metrics.Timer.Context timerContext = metricService.getTimer(MetricType.OXAUTH_USER_AUTHENTICATION_RATE).time();
        try {
            User user = userService.getUser(userName);
            if ((user != null) && checkUserStatus(user)) {
                credentials.setUsername(user.getUserId());
                credentials.setUser(user);
                updateLastLogonUserTime(user);

                authenticated = true;
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

    private User getUserByAttribute(LdapEntryManager ldapAuthEntryManager, String baseDn, String attributeName, String attributeValue) {
        log.debug("Getting user information from LDAP: attributeName = '{0}', attributeValue = '{1}'", attributeName, attributeValue);

        SimpleUser sampleUser = new SimpleUser();
        sampleUser.setDn(baseDn);

        List<CustomAttribute> customAttributes = new ArrayList<CustomAttribute>();
        customAttributes.add(new CustomAttribute(attributeName, attributeValue));

        sampleUser.setCustomAttributes(customAttributes);

        List<User> entries = ldapAuthEntryManager.findEntries(sampleUser, 1);
        log.debug("Found '{0}' entries", entries.size());

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

		log.warn("User '{0}' was disabled", user.getUserId());
        return false;
    }

    private void updateLastLogonUserTime(User user) {
        CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(user.getDn());

        org.xdi.ldap.model.CustomAttribute customAttribute = new org.xdi.ldap.model.CustomAttribute("oxLastLogonTime", new Date());
        customEntry.getCustomAttributes().add(customAttribute);

        try {
            ldapEntryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update oxLastLoginTime of user '{0}'", user.getUserId());
        }
    }

    public void configureSessionUser(SessionId sessionId, Map<String, String> sessionIdAttributes) {
        User user = credentials.getUser();

        SessionId newSessionId;
        if (sessionId == null) {
            newSessionId = sessionIdService.generateAuthenticatedSessionId(user.getDn(), sessionIdAttributes);
        } else {
            newSessionId = sessionIdService.setSessionIdAuthenticated(sessionId, user.getDn());
        }

        configureEventUserContext(newSessionId);
    }

    public SessionId configureEventUser() {
        User user = credentials.getUser();
        if (user == null) {
            return null;
        }

        SessionId sessionId = sessionIdService.generateAuthenticatedSessionId(user.getDn());

        configureEventUserContext(sessionId);

        return sessionId;
    }

    public void configureEventUser(SessionId sessionId) {
        sessionIdService.updateSessionId(sessionId);

        configureEventUserContext(sessionId);
    }

    private void configureEventUserContext(SessionId sessionId) {
        identity.addRole("user");

        Contexts.getEventContext().set("sessionUser", sessionId);
    }

    public void configureSessionClient(Context context) {
        identity.addRole("client");

        Client client = clientService.getClient(credentials.getUsername());
        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        context.set("sessionClient", sessionClient);

        clientService.updatAccessTime(client, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Observer(value = {Constants.EVENT_OXAUTH_CUSTOM_LOGIN_SUCCESSFUL, Identity.EVENT_LOGIN_SUCCESSFUL})
    public void onSuccessfulLogin() {
        log.info("Attempting to redirect user. SessionUser: {0}", sessionUser);

        if ((sessionUser == null) || StringUtils.isBlank(sessionUser.getUserDn())) {
            return;
        }

        User user = userService.getUserByDn(sessionUser.getUserDn());

        log.info("Attempting to redirect user. User: {0}", user);

        if (user != null) {
            final Map<String, String> result = sessionUser.getSessionAttributes();
            Map<String, String> allowedParameters = getAllowedParameters(result);

            result.put("session_id", sessionUser.getId());

            log.trace("Logged in successfully! User: {0}, page: /authorize.xhtml, map: {1}", user, allowedParameters);
            FacesManager.instance().redirect("/authorize.xhtml", (Map) allowedParameters, false);
        }
    }

    public Map<String, String> getAllowedParameters(@Nonnull final Map<String, String> requestParameterMap) {
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

    public User getUserOrRemoveSession(SessionId p_sessionId) {
        if (p_sessionId != null) {
            try {
                if (StringUtils.isNotBlank(p_sessionId.getUserDn())) {
                    final User user = userService.getUserByDn(p_sessionId.getUserDn());
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
        final Map<String, String> parameterMap = new HashMap<String, String>(FacesContext.getCurrentInstance().getExternalContext()
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
        final Object o = Contexts.getEventContext().get(p_name);
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


    public static AuthenticationService instance() {
        return ServerUtil.instance(AuthenticationService.class);
    }

}