/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import io.jans.as.common.model.common.SimpleUser;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.common.UserService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.fido2.service.u2f.util.Constants;
import io.jans.fido2.security.Identity;
import io.jans.fido2.service.shared.MetricService;
import io.jans.model.GluuStatus;
import io.jans.model.SimpleProperty;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.model.metric.MetricType;
import io.jans.model.security.Credentials;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomEntry;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.util.*;

/**
 * Authentication service methods
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version November 23, 2017
 */
@RequestScoped
public class AuthenticationService {

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
    private SessionIdService sessionIdService;

    @Inject
    private MetricService metricService;

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

}