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

import static io.jans.as.model.util.Util.escapeLog;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
        if (logger.isInfoEnabled()) {
            logger.info("Authorizations details to be fetched for userId:{}", escapeLog(userId));
        }

        ClientAuthorization clientAuth = new ClientAuthorization();
        clientAuth.setDn(getClientAuthorizationDn(null));
        clientAuth.setUserId(userId);
        List<ClientAuthorization> authorizations = persistenceEntryManager.findEntries(clientAuth);
        logger.debug("{} client-authorization entries found", authorizations);

        if (authorizations == null || authorizations.isEmpty()) {
            return Collections.emptyMap();
        }

        // Get client id
        Set<String> clientIds = authorizations.stream().map(ClientAuthorization::getClientId)
                .collect(Collectors.toSet());
        logger.debug("clientIds:{}", clientIds);

        // Create a filter based on client Id
        Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
                .collect(Collectors.toList()).toArray(new Filter[] {});
        List<Client> clients = persistenceEntryManager.findEntries(clientService.getDnForClient(null), Client.class,
                Filter.createORFilter(filters));

        Set<String> scopeIds = authorizations.stream().map(ClientAuthorization::getScopes).flatMap(Arrays::stream)
                .collect(Collectors.toSet());
        logger.debug("scopeIds:{}", scopeIds);

        // Do the analog for scopes
        filters = scopeIds.stream().map(id -> Filter.createEqualityFilter("jansId", id)).collect(Collectors.toList())
                .toArray(new Filter[] {});
        List<Scope> scopes = persistenceEntryManager.findEntries(scopeService.getDnForScope(null), Scope.class,
                Filter.createORFilter(filters));

        if (logger.isInfoEnabled()) {
            logger.info("Found {} client authorizations for user:{}", clients.size(), escapeLog(userId));
        }

        Map<Client, Set<Scope>> perms = new HashMap<>();

        for (Client client : clients) {
            Set<Scope> clientScopes = new HashSet<>();
            logger.debug("client:{}", client);
            for (ClientAuthorization auth : authorizations) {
                logger.trace("auth:{}", auth);
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

    public void removeClientAuthorizations(String userId, String clientId, String userName) {
        if (logger.isInfoEnabled()) {
            logger.info("Removing client authorizations for userId:{}, clientId:{}, userName:{}", escapeLog(userId),
                    escapeLog(clientId), escapeLog(userName));
        }

        ClientAuthorization clientAuth = new ClientAuthorization();
        clientAuth.setDn(getClientAuthorizationDn(null));
        clientAuth.setUserId(userId);
        clientAuth.setClientId(clientId);
        logger.debug("clientAuth:{} ", clientAuth);

        List<ClientAuthorization> authorizations = persistenceEntryManager.findEntries(clientAuth);
        logger.debug("{} client-authorization entries found", authorizations);

        if (authorizations == null || authorizations.isEmpty()) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Removing client authorizations for userName:{}", escapeLog(userName));
        }
        authorizations.forEach(authorization -> {
            logger.debug("Deleting ClientAuthorization for id:{}, clientId:{}", authorization.getId(),
                    authorization.getClientId());
            persistenceEntryManager.remove(authorization);
        });

        Token sampleToken = new Token();
        sampleToken.setBaseDn(geTokenDn(null));
        sampleToken.setClientId(clientId);
        sampleToken.setTokenType("refresh_token");
        sampleToken.setUserId(userName);

        logger.info("Removing refresh tokens associated to this user/client pair");
        List<Token> tokens = persistenceEntryManager.findEntries(sampleToken);
        logger.debug("Client tokens:{}", tokens);

        tokens.forEach(token -> {
            logger.trace("Deleting token {}", token.getTokenCode());
            persistenceEntryManager.remove(token);
        });
    }

    public String getClientAuthorizationDn(String id) {
        String baseDn = staticConfiguration.getBaseDn().getAuthorizations();
        if (id == null || StringUtils.isEmpty(id)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", id, baseDn);
    }

    public String geTokenDn(String id) {
        logger.debug("Get Token Dn for id:{}", id);
        String baseDn = staticConfiguration.getBaseDn().getTokens();
        if (id == null || StringUtils.isEmpty(id)) {
            return baseDn;
        }
        return String.format("tknCde=%s,%s", id, baseDn);
    }

    public String getDnForClient(String inum) {
        logger.debug("Get Client Dn for inum:{}", inum);
        String orgDn = organizationService.getDnForOrganization();
        if (inum == null || StringHelper.isEmpty(inum)) {
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
        logger.info("scopeIds:{}", scopeIds);

        return scopeService.searchScopesById(scopeIds);

    }

}
