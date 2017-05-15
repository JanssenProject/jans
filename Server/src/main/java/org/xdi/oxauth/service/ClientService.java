/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.google.common.collect.Sets;
import com.unboundid.ldap.sdk.Filter;
import org.codehaus.jettison.json.JSONArray;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.python.jline.internal.Preconditions;
import org.slf4j.Logger;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.ldap.model.SearchScope;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.service.CacheService;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

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

	public static final String[] CLIENT_OBJECT_CLASSES = new String[] { "oxAuthClient" };

    private static final String CACHE_CLIENT_NAME = "ClientCache";
    private static final String CACHE_CLIENT_FILTER_NAME = "ClientFilterCache";

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private CacheService cacheService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private ClientFilterService clientFilterService;
    
    @Inject
    private EncryptionService encryptionService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    private static String getClientIdCacheKey(String clientId) {
        return "client_id_" + StringHelper.toLowerCase(clientId);
    }

    private static String getClientDnCacheKey(String dn) {
        return "client_dn_" + StringHelper.toLowerCase(dn);
    }

    public void persist(Client client) {
        ldapEntryManager.persist(client);
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
            String decryptedClientSecret = decryptSecret(client.getClientSecret());
            authenticated = client != null && decryptedClientSecret != null
                    && decryptedClientSecret.equals(password);
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
                set.add(getClient(clientId));
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

    public Client getClient(String clientId, String registrationAccessToken) {
        String baseDN = staticConfiguration.getBaseDn().getClients();

        Filter filterInum = Filter.createEqualityFilter("inum", clientId);
        Filter registrationAccessTokenInum = Filter.createEqualityFilter("oxAuthRegistrationAccessToken", registrationAccessToken);
        Filter filter = Filter.createANDFilter(filterInum, registrationAccessTokenInum);

        List<Client> clients = ldapEntryManager.findEntries(baseDN, Client.class, filter, null, 1, 1);
        if (clients != null && clients.size() > 0) {
            return clients.get(0);
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
        Client client = fromCache(dn);
        if (client == null) {
            try {
                client = ldapEntryManager.find(Client.class, dn);
                putInCache(client);
            } catch (Exception ex) {
                log.debug(ex.getMessage());
            }
        } else {
            log.trace("Get client from cache by Dn '{}'", dn);
        }

        return client;
    }

    private void putInCache(Client client) {
    	if (client == null) {
    		return;
    	}

        try {
            cacheService.put(CACHE_CLIENT_FILTER_NAME, getClientIdCacheKey(client.getClientId()), client);
            cacheService.put(CACHE_CLIENT_NAME, getClientDnCacheKey(client.getDn()), client);
        } catch (Exception e) {
            log.error("Failed to put client in cache, client:" + client, e);
        }
    }

    private Client fromCache(String dn) {
        try {
            String key = getClientDnCacheKey(dn);
            return (Client) cacheService.get(CACHE_CLIENT_NAME, key);
        } catch (Exception e) {
            log.error("Failed to fetch client from cache, dn: " + dn, e);
            return null;
        }
    }

    public org.xdi.ldap.model.CustomAttribute getCustomAttribute(Client client, String attributeName) {
        for (org.xdi.ldap.model.CustomAttribute customAttribute : client.getCustomAttributes()) {
            if (StringHelper.equalsIgnoreCase(attributeName, customAttribute.getName())) {
                return customAttribute;
            }
        }

        return null;
    }

    public void setCustomAttribute(Client client, String attributeName, String attributeValue) {
        org.xdi.ldap.model.CustomAttribute customAttribute = getCustomAttribute(client, attributeName);

        if (customAttribute == null) {
            customAttribute = new org.xdi.ldap.model.CustomAttribute(attributeName);
            client.getCustomAttributes().add(customAttribute);
        }

        customAttribute.setValue(attributeValue);
    }

    public List<Client> getAllClients(String[] returnAttributes) {
        String baseDn = staticConfiguration.getBaseDn().getClients();

        List<Client> result = ldapEntryManager.findEntries(baseDn, Client.class, returnAttributes, null);

        return result;
    }

    public List<Client> getAllClients(String[] returnAttributes, int size) {
        String baseDn = staticConfiguration.getBaseDn().getClients();

        List<Client> result = ldapEntryManager.findEntries(baseDn, Client.class, null, returnAttributes, size, size);

        return result;
    }

    public List<Client> getClientsWithExpirationDate(BatchOperation<Client> batchOperation, int searchLimit, int sizeLimit){
        String baseDN = staticConfiguration.getBaseDn().getClients();
        Filter filter = Filter.createPresenceFilter("oxAuthClientSecretExpiresAt");
        return ldapEntryManager.findEntries(baseDN, Client.class, filter, SearchScope.SUB, null, batchOperation, 0, searchLimit, sizeLimit);
    }

    public String buildClientDn(String p_clientId) {
        final StringBuilder dn = new StringBuilder();
        dn.append(String.format("inum=%s,", p_clientId));
        dn.append(staticConfiguration.getBaseDn().getClients()); // ou=clients,o=@!1111,o=gluu
        return dn.toString();
    }

    public void remove(Client client) {
        if (client != null) {
            removeFromCache(client);

            String clientDn = client.getDn();
            ldapEntryManager.removeWithSubtree(clientDn);
        }
    }

    private void removeFromCache(Client client) {
        try {
            String clientId = client.getClientId();
            String clientDn = client.getDn();

            cacheService.remove(CACHE_CLIENT_FILTER_NAME, getClientIdCacheKey(clientId));
            cacheService.remove(CACHE_CLIENT_NAME, getClientDnCacheKey(clientDn));
        } catch (Exception e) {
            log.error("Failed to remove client from cache.", e);
        }
    }

    public void updatAccessTime(Client client, boolean isUpdateLogonTime) {
		if (!appConfiguration.getUpdateClientAccessTime()) {
			return;
		}

		String clientDn = client.getDn();

        CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(clientDn);
		customEntry.setCustomObjectClasses(CLIENT_OBJECT_CLASSES);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        CustomAttribute customAttributeLastAccessTime = new CustomAttribute("oxLastAccessTime", now);
        customEntry.getCustomAttributes().add(customAttributeLastAccessTime);

        if (isUpdateLogonTime) {
            CustomAttribute customAttributeLastLogonTime = new CustomAttribute("oxLastLogonTime", now);
            customEntry.getCustomAttributes().add(customAttributeLastLogonTime);
        }

        try {
            ldapEntryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update oxLastAccessTime and oxLastLoginTime of client '{}'", clientDn);
        }

        removeFromCache(client);
    }

    public Object getAttribute(Client client, String clientAttribute) throws InvalidClaimException {
        Object attribute = null;

        if (clientAttribute != null) {
            if (clientAttribute.equals("displayName")) {
                attribute = client.getClientName();
            } else if (clientAttribute.equals("inum")) {
                attribute = client.getClientId();
            } else if (clientAttribute.equals("oxAuthAppType")) {
                attribute = client.getApplicationType();
            } else if (clientAttribute.equals("oxAuthIdTokenSignedResponseAlg")) {
                attribute = client.getIdTokenSignedResponseAlg();
            } else if (clientAttribute.equals("oxAuthRedirectURI") && client.getRedirectUris() != null) {
                JSONArray array = new JSONArray();
                for (String redirectUri : client.getRedirectUris()) {
                    array.put(redirectUri);
                }
                attribute = array;
            } else if (clientAttribute.equals("oxAuthScope") && client.getScopes() != null) {
                JSONArray array = new JSONArray();
                for (String scopeDN : client.getScopes()) {
                    Scope s = scopeService.getScopeByDn(scopeDN);
                    if (s != null) {
                        String scopeName = s.getDisplayName();
                        array.put(scopeName);
                    }
                }
                attribute = array;
            } else {
                for (CustomAttribute customAttribute : client.getCustomAttributes()) {
                    if (customAttribute.getName().equals(clientAttribute)) {
                        List<String> values = customAttribute.getValues();
                        if (values != null) {
                            if (values.size() == 1) {
                                attribute = values.get(0);
                            } else {
                                JSONArray array = new JSONArray();
                                for (String v : values) {
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

}