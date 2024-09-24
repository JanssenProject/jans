package io.jans.configapi.service.auth;

import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.ClientAuthorization;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.model.Token;
import io.jans.model.SearchRequest;
import io.jans.model.token.TokenEntity;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import static io.jans.as.model.util.Util.escapeLog;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

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

    public PagedResult<TokenEntity> searchToken(SearchRequest searchRequest) {
        logger.debug("Search Token with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter grantIdFilter = Filter.createSubstringFilter("grtId", null, targetArray, null);
                Filter userIdFilter = Filter.createSubstringFilter("usrId", null, targetArray, null);
                Filter userDnFilter = Filter.createSubstringFilter("jansUsrDN", null, targetArray, null);
                Filter clientIdFilter = Filter.createSubstringFilter("clnId", null, targetArray, null);
                Filter scopeFilter = Filter.createSubstringFilter("scp", null, targetArray, null);
                Filter tokenTypeFilter = Filter.createSubstringFilter("tknTyp", null, targetArray, null);
                Filter grantTypeFilter = Filter.createSubstringFilter("grtTyp", null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);
                filters.add(Filter.createORFilter(grantIdFilter, userIdFilter, userDnFilter, clientIdFilter,
                        scopeFilter, tokenTypeFilter, grantTypeFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.trace("Token pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("Token dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.debug("Token searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForClient(null), TokenEntity.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public void revokeTokenEntity(String tknCde) {
        if (logger.isInfoEnabled()) {
            logger.info(" Revoke token - tknCde:{}", escapeLog(tknCde));
        }

        TokenEntity tokenEntity = this.getTokenEntityByCode(tknCde);
        logger.debug("Token to be revoked identified by tknCde:{} is:{}", tokenEntity, tknCde);

        if (tokenEntity == null) {
            throw new NotFoundException("Could not find Token identified by - " + tknCde);
        }

        persistenceEntryManager.removeRecursively(tokenEntity.getDn(), TokenEntity.class);
    }

    public TokenEntity getTokenEntityByCode(String tknCde) {
        TokenEntity tokenEntity = null;
        try {
            tokenEntity = persistenceEntryManager.find(TokenEntity.class, geTokenDn(tknCde));
        } catch (Exception ex) {
            logger.error("Failed to get Token identified by tknCde:{" + tknCde + "}", ex);
        }
        return tokenEntity;
    }

}
