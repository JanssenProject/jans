package org.xdi.oxauth.service;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
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
import org.xdi.model.SimpleProperty;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.CacheService;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Blum Date: 10.24.2011
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

    public Client getClient(String clientId) {
        if (clientId != null && !clientId.isEmpty()) {
            if (Boolean.TRUE.equals(ConfigurationFactory.getConfiguration().getClientAuthenticationFiltersEnabled())) {
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
		String key = "client_id_" + StringHelper.toLowerCase(clientId);
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
        String baseDN = ConfigurationFactory.getBaseDn().getClients();

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

    /**
     * Returns client by DN.
     *
     * @param dn dn of client
     * @return Client
     */
    public Client getClientByDn(String dn) {
		String key = "client_dn_" + StringHelper.toLowerCase(dn);
		Client client = (Client) cacheService.get(CACHE_CLIENT_NAME, key);
		if (client == null) {
			client = ldapEntryManager.find(Client.class, dn);
			cacheService.put(CACHE_CLIENT_NAME, key, client);
		} else {
			log.trace("Get client from cache by Dn '{0}'", dn);
		}

		return client;
    }

    public List<Client> getClientsWithExpirationDate() {
        String baseDN = ConfigurationFactory.getBaseDn().getClients();
        Filter filter = Filter.createPresenceFilter("oxAuthClientExpirationDate");

        return ldapEntryManager.findEntries(baseDN, Client.class, filter);
    }

    public void remove(Client client) {
        if (client != null) {
            ldapEntryManager.removeWithSubtree(client.getDn());
        }
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

}