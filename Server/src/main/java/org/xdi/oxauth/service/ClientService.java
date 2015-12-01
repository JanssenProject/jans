/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.google.common.collect.Sets;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.python.jline.internal.Preconditions;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.model.SimpleProperty;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.CacheService;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;

import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Blum Date: 10.24.2011
 * @author Yuriy Movchan Date: 04/15/2014
 */
@Scope(ScopeType.STATELESS)
@Name("clientService")
@AutoCreate
public class ClientService {

    public static final String EVENT_CLEAR_CLIENT_CACHE = "eventClearClient";
	private static final String CACHE_CLIENT_NAME = "ClientCache";
	private static final String CACHE_CLIENT_FILTER_NAME = "ClientFilterCache";

	@Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

	@In
	private CacheService cacheService;

    @In
    private ClientFilterService clientFilterService;

    /**
     * Get ClientService instance
     *
     * @return ClientService instance
     */
    public static ClientService instance() {
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            Lifecycle.beginCall();
        }

        return ServerUtil.instance(ClientService.class);
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
        log.debug("Authenticating Client with LDAP: clientId = {0}", clientId);
        boolean authenticated = false;

        try {
            Client client = getClient(clientId);
            authenticated = client != null && client.getClientSecret() != null
                    && client.getClientSecret().equals(password);
        } catch (StringEncrypter.EncryptionException e) {
            log.error(e.getMessage(), e);
        }

        return authenticated;
    }

    public Set<Client> getClient(Collection<String> clientIds, boolean silent) {
        Set<Client> set = Sets.newHashSet();
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
            if (Boolean.TRUE.equals(ConfigurationFactory.instance().getConfiguration().getClientAuthenticationFiltersEnabled())) {
                final String dn = getClientDnByFilters(clientId);
                if (StringUtils.isNotBlank(dn)) {
                    return getClientByDn(dn);
                }
            }

            Client result = getClientByDn(Client.buildClientDn(clientId));
            log.debug("Found {0} entries for client id = {1}", result != null ? 1 : 0, clientId);

            return result;
        }
        return null;
    }

	private String getClientDnByFilters(String clientId) {
		String key = getClientIdCacheKey(clientId);
		SimpleProperty simpleProperty = (SimpleProperty) cacheService.get(CACHE_CLIENT_FILTER_NAME, key);
		if (simpleProperty == null) {
			simpleProperty = new SimpleProperty(clientFilterService.processFilters(clientId));
			cacheService.put(CACHE_CLIENT_FILTER_NAME, key, simpleProperty);
		} else {
			log.trace("Get client Dn '{0}' from cache by Id '{1}'", simpleProperty.getValue(), clientId);
		}

		return simpleProperty.getValue();
	}

    public Client getClient(String clientId, String registrationAccessToken) {
        String baseDN = ConfigurationFactory.instance().getBaseDn().getClients();

        Client client = new Client();
        client.setDn(baseDN);
        client.setClientId(clientId);
        client.setRegistrationAccessToken(registrationAccessToken);

        List<Client> clients = ldapEntryManager.findEntries(client);
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
		String key = getClientDnCacheKey(dn);
		Client client = (Client) cacheService.get(CACHE_CLIENT_NAME, key);
		if (client == null) {
			client = ldapEntryManager.find(Client.class, dn);
			cacheService.put(CACHE_CLIENT_NAME, key, client);
		} else {
			log.trace("Get client from cache by Dn '{0}'", dn);
		}

		return client;
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
        String baseDn = ConfigurationFactory.instance().getBaseDn().getClients();

        List<Client> result = ldapEntryManager.findEntries(baseDn, Client.class, returnAttributes, null);

		return result;
	}

    public List<Client> getClientsWithExpirationDate(String[] returnAttributes) {
        String baseDN = ConfigurationFactory.instance().getBaseDn().getClients();
        Filter filter = Filter.createPresenceFilter("oxAuthClientSecretExpiresAt");

        return ldapEntryManager.findEntries(baseDN, Client.class, filter);
    }

    public void remove(Client client) {
        if (client != null) {
        	removeFromCache(client);
        	
        	String clientDn = client.getDn();
            ldapEntryManager.removeWithSubtree(clientDn);
        }
    }

	private void removeFromCache(Client client) {
		String clientId = client.getClientId();
		String clientDn = client.getDn();

		cacheService.remove(CACHE_CLIENT_FILTER_NAME, getClientIdCacheKey(clientId));
		cacheService.remove(CACHE_CLIENT_NAME, getClientDnCacheKey(clientDn));
	}

    /**
	 * Remove all clients from caches after receiving event
	 */
	@Observer(EVENT_CLEAR_CLIENT_CACHE)
	public void clearClientCache() {
		log.debug("Clearing up clients cache");
		cacheService.removeAll(CACHE_CLIENT_NAME);
		cacheService.removeAll(CACHE_CLIENT_FILTER_NAME);
	}

	public void updatAccessTime(Client client, boolean isUpdateLogonTime) {
		String clientDn = client.getDn();

		CustomEntry customEntry = new CustomEntry();
		customEntry.setDn(clientDn);

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
		    log.error("Failed to update oxLastAccessTime and oxLastLoginTime of client '{0}'", clientDn);
		}
		
		removeFromCache(client);
	}

	private String getClientIdCacheKey(String clientId) {
		return "client_id_" + StringHelper.toLowerCase(clientId);
	}

	private String getClientDnCacheKey(String dn) {
		return "client_dn_" + StringHelper.toLowerCase(dn);
	}

}