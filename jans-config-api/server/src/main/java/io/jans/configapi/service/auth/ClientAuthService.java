package io.jans.configapi.service.auth;

import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.as.persistence.model.ClientAuthorization;
import io.jans.orm.PersistenceEntryManager;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.model.Token;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ClientAuthService {

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    StaticConfiguration staticConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    ScopeService scopeService;

    @Inject
    private OrganizationService organizationService;

    public Map<Client, Set<Scope>> getUserAuthorizations(String userId) {

        logger.info(" Authorizations details to be fetched for userId:{} ", userId);

        ClientAuthorization clientAuth = new ClientAuthorization();
        clientAuth.setDn(getClientAuthorizationDn(null));
        clientAuth.setId(userId);
        List<ClientAuthorization> authorizations = persistenceEntryManager.findEntries(clientAuth);
        logger.info("{} client-authorization entries found", authorizations);

        if (authorizations == null || authorizations.isEmpty()) {
            return Collections.emptyMap();
        }

        // Get client id
        Set<String> clientIds = authorizations.stream().map(ClientAuthorization::getClientId)
                .collect(Collectors.toSet());
        logger.info("clientIds:{}", clientIds);


        // Create a filter based on client Id
        Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
                .collect(Collectors.toList()).toArray(new Filter[] {});
        List<Client> clients = persistenceEntryManager.findEntries(clientService.getDnForClient(null), Client.class,
                Filter.createORFilter(filters));

        Set<String> scopeIds = authorizations.stream().map(ClientAuthorization::getScopes).flatMap(Arrays::stream)
                .collect(Collectors.toSet());
        logger.info("scopeIds:{}", scopeIds);
        
        // Do the analog for scopes
        filters = scopeIds.stream().map(id -> Filter.createEqualityFilter("jansId", id)).collect(Collectors.toList())
                .toArray(new Filter[] {});
        List<Scope> scopes = persistenceEntryManager.findEntries(scopeService.getDnForScope(null), Scope.class,
                Filter.createORFilter(filters));

        logger.info("Found {} client authorizations for user {}", clients.size(), userId);
        Map<Client, Set<Scope>> perms = new HashMap<>();

        for (Client client : clients) {
            Set<Scope> clientScopes = new HashSet<>();
            logger.debug("client:{}", client);
            for (ClientAuthorization auth : authorizations) {
                logger.debug("auth:{}", auth);
                if (auth.getClientId().equals(client.getClientId())) {
                    for (String scopeName : auth.getScopes()) {
                        scopes.stream().filter(sc -> sc.getId().equals(scopeName)).findAny()
                                .ifPresent(clientScopes::add);
                    }
                }
            }
            perms.put(client, clientScopes);
        }
        logger.info("perms {}", perms);
        return perms;
    }

    public void removeClientAuthorizations(String userId, String userName, String clientId) {
        logger.info("Removing client authorizations for userId:{}, userName:{}, clientId:{}", userId, userName,
                clientId);

        ClientAuthorization clientAuth = new ClientAuthorization();
        clientAuth.setClientId(clientId);
        clientAuth.setUserId(userId);
        clientAuth.setDn(getClientAuthorizationDn(userId));
        logger.debug("client-authorization entries filter:{}", clientAuth);

        List<ClientAuthorization> authorizations = persistenceEntryManager.findEntries(clientAuth);
        logger.debug("{} client-authorization entries found", authorizations);

        if (authorizations == null || authorizations.isEmpty()) {
            return;
        }

        logger.info("Removing client authorizations for user {}", userName);

        authorizations.forEach(authorization -> {
            logger.info("Deleting ClientAuthorization for id:{}, clientId:{}", authorization.getId(),
                    authorization.getClientId());
            persistenceEntryManager.remove(authorization);
        });

        Token sampleToken = new Token();
        sampleToken.setBaseDn(geTokenDn(null));
        sampleToken.setClientId(clientId);
        sampleToken.setTokenType("refresh_token");
        sampleToken.setUserId(userName);

        logger.info("Removing refresh tokens associated to this user/client pair");
        // Here we ignore the return value of deletion
        List<Token> tokens = persistenceEntryManager.findEntries(sampleToken);

        tokens.forEach(token -> {
            logger.debug("Deleting token {}", token.getTokenCode());
            persistenceEntryManager.remove(token);
        });

    }

    public String getClientAuthorizationDn(String id) {
        String baseDn = staticConfiguration.getBaseDn().getAuthorizations();
        if (StringUtils.isEmpty(id)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", id, baseDn);
    }

    public String geTokenDn(String id) {
        String baseDn = staticConfiguration.getBaseDn().getTokens();
        if (StringUtils.isEmpty(id)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", id, baseDn);
    }

    public String getDnForClient(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

    public List<Scope> getScopeList(List<ClientAuthorization> clientAuthorizations) {
        logger.info("Client authorizations for clientAuthorizations {}", clientAuthorizations);
        Set<String> scopeIds = new HashSet<>();
        for (ClientAuthorization auth : clientAuthorizations) {
            logger.debug("Client authorizations for auth.getClientId():{}, auth.getScopes():{}", auth.getClientId(),
                    auth.getScopes());
            if (auth.getScopes() != null && auth.getScopes().length > 0) {
                Arrays.asList(auth.getScopes()).stream().filter(scopeIds::add);
            }
        }
        logger.debug("scopeIds:{}", scopeIds);

        return scopeService.searchScopesById(scopeIds);

    }

}
