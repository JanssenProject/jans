/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.Scope;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomEntry;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.json.JSONArray;
import org.python.jline.internal.Preconditions;
import org.slf4j.Logger;

import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 04/15/2014
 * @version October 22, 2016
 */
@Stateless
@Named
public class ClientService {

    protected static final String[] CLIENT_OBJECT_CLASSES = new String[]{"jansClnt"};

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private CacheService cacheService;

    @Inject
    private LocalCacheService localCacheService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private EncryptionService encryptionService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    public void persist(Client client) {
    	ignoreCustomObjectClassesForNonLDAP(client);
        ldapEntryManager.persist(client);
    }

    private Client ignoreCustomObjectClassesForNonLDAP(Client client) {
        String persistenceType = ldapEntryManager.getPersistenceType();
        log.debug("persistenceType: {}", persistenceType);
        if (!PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType)) {
        	log.debug(
                    "Setting CustomObjectClasses :{} to null as it's used only for LDAP and current persistenceType is {} ",
                    client.getCustomObjectClasses(), persistenceType);
            client.setCustomObjectClasses(null);
        }

        return client;
    }

    public void merge(Client client) {
        ldapEntryManager.merge(client);
        removeFromCache(client);
    }

    /**
     * Authenticate client.
     *
     * @param clientId Client inum.
     * @param password Client password.
     * @return <code>true</code> if success, otherwise <code>false</code>.
     */
    public boolean authenticate(String clientId, String password) {
        log.debug("Authenticating Client with LDAP: clientId = {}", clientId);
        boolean authenticated = false;

        try {
            Client client = getClient(clientId);
            if (client == null) {
                log.debug("Failed to find client = {}", clientId);
                return authenticated;
            }
            String decryptedClientSecret = decryptSecret(client.getClientSecret());
            authenticated = decryptedClientSecret != null && decryptedClientSecret.equals(password);
        } catch (StringEncrypter.EncryptionException e) {
            log.error(e.getMessage(), e);
        }

        return authenticated;
    }

    public Set<Client> getClient(Collection<String> clientIds, boolean silent) {
        Set<Client> set = Sets.newHashSet();

        if (clientIds == null) {
            return set;
        }

        for (String clientId : clientIds) {
            try {
                Client client = getClient(clientId);
                if (client != null) {
                    set.add(client);
                }
            } catch (RuntimeException e) {
                if (!silent) {
                    throw e;
                }
            }
        }
        return set;
    }

    public Client getClient(String clientId) {
        if (clientId != null && !clientId.isEmpty()) {
            Client result = getClientByDn(buildClientDn(clientId));
            log.debug("Found {} entries for client id = {}", result != null ? 1 : 0, clientId);

            return result;
        }
        return null;
    }

    public boolean isPublic(String clientId) {
        return isPublic(getClient(clientId));
    }

    public boolean isPublic(Client client) {
        return client != null && client.getAuthenticationMethod() == AuthenticationMethod.NONE;
    }

    public Client getClient(String clientId, String registrationAccessToken) {
        final Client client = getClient(clientId);
        if (client != null && registrationAccessToken != null && registrationAccessToken.equals(client.getRegistrationAccessToken())) {
            return client;
        }
        return null;
    }

    public Set<Client> getClientsByDns(Collection<String> dnList) {
        return getClientsByDns(dnList, true);
    }

    public Set<Client> getClientsByDns(Collection<String> dnList, boolean silently) {
        Preconditions.checkNotNull(dnList);

        final Set<Client> result = Sets.newHashSet();
        for (String clientDn : dnList) {
            try {
                result.add(getClientByDn(clientDn));
            } catch (RuntimeException e) {
                if (!silently) {
                    throw e;
                }
            }
        }
        return result;
    }

    /**
     * Returns client by DN.
     *
     * @param dn dn of client
     * @return Client
     */
    public Client getClientByDn(String dn) {
        BaseCacheService usedCacheService = getCacheService();
        try {
            return usedCacheService.getWithPut(dn, () -> ldapEntryManager.find(Client.class, dn), 60);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    public CustomObjectAttribute getCustomAttribute(Client client, String attributeName) {
        for (CustomObjectAttribute customAttribute : client.getCustomAttributes()) {
            if (StringHelper.equalsIgnoreCase(attributeName, customAttribute.getName())) {
                return customAttribute;
            }
        }

        return null;
    }

    public void setCustomAttribute(Client client, String attributeName, String attributeValue) {
        CustomObjectAttribute customAttribute = getCustomAttribute(client, attributeName);

        if (customAttribute == null) {
            customAttribute = new CustomObjectAttribute(attributeName);
            client.getCustomAttributes().add(customAttribute);
        }

        customAttribute.setValue(attributeValue);
    }

    public List<Client> getAllClients(String[] returnAttributes) {
        String baseDn = staticConfiguration.getBaseDn().getClients();

        return ldapEntryManager.findEntries(baseDn, Client.class, null, returnAttributes);
    }

    public List<Client> getAllClients(String[] returnAttributes, int size) {
        String baseDn = staticConfiguration.getBaseDn().getClients();

        return ldapEntryManager.findEntries(baseDn, Client.class, null, returnAttributes, size);
    }

    public String buildClientDn(String clientId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("inum=%s,", clientId));
        dn.append(staticConfiguration.getBaseDn().getClients()); // ou=clients,o=jans
        return dn.toString();
    }

    public void remove(Client client) {
        if (client != null) {
            removeFromCache(client);

            String clientDn = client.getDn();
            ldapEntryManager.removeRecursively(clientDn, Client.class);
        }
    }

    public void removeFromCache(Client client) {
        BaseCacheService usedCacheService = getCacheService();
        try {
            usedCacheService.remove(client.getDn());
        } catch (Exception e) {
            log.error("Failed to remove client from cache." + client.getDn(), e);
        }
    }

    public void updateAccessTime(Client client, boolean isUpdateLogonTime) {
        if (isFalse(appConfiguration.getUpdateClientAccessTime())) {
            return;
        }

        String clientDn = client.getDn();

        CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(clientDn);
        customEntry.setCustomObjectClasses(CLIENT_OBJECT_CLASSES);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        String nowDateString = ldapEntryManager.encodeTime(customEntry.getDn(), now);

        CustomAttribute customAttributeLastAccessTime = new CustomAttribute("jansLastAccessTime", nowDateString);
        customEntry.getCustomAttributes().add(customAttributeLastAccessTime);

        if (isUpdateLogonTime) {
            CustomAttribute customAttributeLastLogonTime = new CustomAttribute("jansLastLogonTime", nowDateString);
            customEntry.getCustomAttributes().add(customAttributeLastLogonTime);
        }

        try {
            ldapEntryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update jansLastAccessTime and jansLastLogonTime of client '{}'", clientDn);
            log.trace("Failed to update user:", epe);
        }

        removeFromCache(client);
    }

    public Object getAttribute(Client client, String clientAttribute) {
        Object attribute = null;

        if (clientAttribute != null) {
            if (clientAttribute.equals("displayName")) {
                attribute = client.getClientName();
            } else if (clientAttribute.equals("inum")) {
                attribute = client.getClientId();
            } else if (clientAttribute.equals("jansAppTyp")) {
                attribute = client.getApplicationType();
            } else if (clientAttribute.equals("jansIdTknSignedRespAlg")) {
                attribute = client.getIdTokenSignedResponseAlg();
            } else if (clientAttribute.equals("jansRedirectURI") && client.getRedirectUris() != null) {
                JSONArray array = new JSONArray();
                for (String redirectUri : client.getRedirectUris()) {
                    array.put(redirectUri);
                }
                attribute = array;
            } else if (clientAttribute.equals("jansScope") && client.getScopes() != null) {
                JSONArray array = new JSONArray();
                for (String scopeDN : client.getScopes()) {
                    Scope s = scopeService.getScopeByDn(scopeDN);
                    if (s != null) {
                        String scopeName = s.getId();
                        array.put(scopeName);
                    }
                }
                attribute = array;
            } else {
                for (CustomObjectAttribute customAttribute : client.getCustomAttributes()) {
                    if (customAttribute.getName().equals(clientAttribute)) {
                        List<Object> values = customAttribute.getValues();
                        if (values != null) {
                            if (values.size() == 1) {
                                attribute = values.get(0);
                            } else {
                                JSONArray array = new JSONArray();
                                for (Object v : values) {
                                    array.put(v);
                                }
                                attribute = array;
                            }
                        }

                        break;
                    }
                }
            }
        }

        return attribute;
    }

    public String decryptSecret(String encryptedClientSecret) throws EncryptionException {
        return encryptionService.decrypt(encryptedClientSecret);
    }

    public String encryptSecret(String clientSecret) throws EncryptionException {
        return encryptionService.encrypt(clientSecret);
    }

    private BaseCacheService getCacheService() {
        if (isTrue(appConfiguration.getUseLocalCache())) {
            return localCacheService;
        }

        return cacheService;
    }
}