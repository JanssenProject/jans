package io.jans.casa.plugins.consent.service;

import io.jans.orm.search.filter.Filter;

import io.jans.casa.plugins.consent.model.ClientAuthorization;
import io.jans.casa.core.model.Client;
import io.jans.casa.core.model.Scope;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.consent.model.Token;
import io.jans.casa.service.IPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles client authorizations for a user
 */
public class ClientAuthorizationsService {

    private static final String TOKENS_DN = "ou=tokens,o=jans";
    private static final String AUTHORIZATIONS_DN = "ou=authorizations,o=jans";

    private Logger logger = LoggerFactory.getLogger(getClass());
    private IPersistenceService persistenceService;

    public ClientAuthorizationsService() {
        persistenceService = Utils.managedBean(IPersistenceService.class);
    }

    public Map<Client, Set<Scope>> getUserClientPermissions(String userId) {

        ClientAuthorization caSample = new ClientAuthorization();
        caSample.setBaseDn(AUTHORIZATIONS_DN);
        caSample.setUserId(userId);
        List<ClientAuthorization> authorizations = persistenceService.find(caSample);

        int caEntries = authorizations.size();
        logger.debug("{} client-authorization entries found", caEntries);
        
        if (caEntries == 0) return Collections.emptyMap();

        //Obtain client ids from all this user's client authorizations
        Set<String> clientIds = authorizations.stream().map(ClientAuthorization::getJansClntId).collect(Collectors.toSet());

        //Create a filter based on client Ids, alternatively one can make n queries to obtain client references one by one
        Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
                .collect(Collectors.toList()).toArray(new Filter[]{});
        List<Client> clients = persistenceService.find(Client.class, persistenceService.getClientsDn(), Filter.createORFilter(filters));

        Set<String> scopeIds = authorizations.stream().map(ClientAuthorization::getScopes).flatMap(List::stream).collect(Collectors.toSet());

        //Do the analog for scopes
        filters = scopeIds.stream().map(id -> Filter.createEqualityFilter("jansId", id))
                .collect(Collectors.toList()).toArray(new Filter[]{});
        List<Scope> scopes = persistenceService.find(Scope.class, persistenceService.getScopesDn(), Filter.createORFilter(filters));

        logger.info("Found {} client authorizations for user {}", clients.size(), userId);
        Map<Client, Set<Scope>> perms = new HashMap<>();

        for (Client client : clients) {
            Set<Scope> clientScopes = new HashSet<>();

            for (ClientAuthorization auth : authorizations) {
                if (auth.getJansClntId().equals(client.getInum())) {
                    for (String scopeName : auth.getScopes()) {
                        scopes.stream().filter(sc -> sc.getId().equals(scopeName)).findAny().ifPresent(clientScopes::add);
                    }
                }
            }
            perms.put(client, clientScopes);
        }

        return perms;

    }

    public void removeClientAuthorizations(String userId, String userName, String clientId) {

        ClientAuthorization caSample = new ClientAuthorization();
        caSample.setJansClntId(clientId);
        caSample.setBaseDn(AUTHORIZATIONS_DN);
        caSample.setUserId(userId);

        logger.info("Removing client authorizations for user {}", userName);
        //Here we ignore the return value of deletion
        persistenceService.find(caSample).forEach(persistenceService::delete);

        Token sampleToken = new Token();
        sampleToken.setBaseDn(TOKENS_DN);
        sampleToken.setClientId(clientId);
        sampleToken.setTokenType("refresh_token");
        sampleToken.setUserId(userName);

        logger.info("Removing refresh tokens associated to this user/client pair");
        //Here we ignore the return value of deletion
        persistenceService.find(sampleToken).forEach(token -> {
                    logger.debug("Deleting token {}", token.getTokenCode());
                    persistenceService.delete(token);
                });

    }

}
