package org.xdi.oxauth.model.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.registration.Client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/01/2013
 */

public class AuthorizationGrantListInMemory implements TokenIssuerObserver, IAuthorizationGrantList {

    private static final Log LOGGER = Logging.getLog(AuthorizationGrantListInMemory.class);

    private Map<String, AuthorizationGrant> authorizationGrantsByCode;
    private Map<String, AuthorizationGrant> authorizationGrantsByIdToken;
    private Map<String, AuthorizationGrant> authorizationGrantsByAccessToken;
    private Map<String, AuthorizationGrant> authorizationGrantsByRefreshToken;
    private Multimap<String, AuthorizationGrant> authorizationGrantsByClientId;

    /**
     * Constructor
     */
    public AuthorizationGrantListInMemory() {
        authorizationGrantsByCode = new ConcurrentHashMap<String, AuthorizationGrant>();
        authorizationGrantsByIdToken = new ConcurrentHashMap<String, AuthorizationGrant>();
        authorizationGrantsByAccessToken = new ConcurrentHashMap<String, AuthorizationGrant>();
        authorizationGrantsByRefreshToken = new ConcurrentHashMap<String, AuthorizationGrant>();
        authorizationGrantsByClientId = Multimaps.synchronizedSetMultimap(HashMultimap.<String, AuthorizationGrant>create());
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrants() {
        return new ArrayList<AuthorizationGrant>(authorizationGrantsByClientId.values());
    }

    @Override
    public void removeAuthorizationGrants(List<AuthorizationGrant> authorizationGrants) {
        for (AuthorizationGrant authorizationGrant : authorizationGrants) {
            if (authorizationGrant instanceof AuthorizationCodeGrant) {
                AuthorizationCode authorizationCode = ((AuthorizationCodeGrant) authorizationGrant).getAuthorizationCode();
                authorizationGrantsByCode.remove(authorizationCode.getCode());
            }
            if (authorizationGrant.getIdToken() != null) {
                authorizationGrantsByIdToken.remove(authorizationGrant.getIdToken().getCode());
            }
            for (String code : authorizationGrant.getAccessTokensCodes()) {
                authorizationGrantsByAccessToken.remove(code);
            }
            if (authorizationGrant.getLongLivedAccessToken() != null) {
                authorizationGrantsByAccessToken.remove(authorizationGrant.getLongLivedAccessToken().getCode());
            }
            for (String code : authorizationGrant.getRefreshTokensCodes()) {
                authorizationGrantsByRefreshToken.remove(code);
            }
            if (authorizationGrant.getClient() != null) {
                authorizationGrantsByClientId.remove(authorizationGrant.getClient().getClientId(), authorizationGrant);
            }
        }
    }

    @Override
    public void addAuthorizationGrant(AuthorizationGrant authorizationGrant) {
        if (authorizationGrant != null) {
            final IAuthorizationGrant grant = authorizationGrant.getGrant();
            if (grant instanceof AuthorizationGrantInMemory) {
                AuthorizationGrantInMemory inMemory = (AuthorizationGrantInMemory) grant;
                inMemory.setTokenIssuerObserver(this);

                if (authorizationGrant instanceof AuthorizationCodeGrant) {
                    AuthorizationCode authorizationCode = authorizationGrant.getAuthorizationCode();
                    indexByAuthorizationCode(authorizationCode, authorizationGrant);
                }
                if (authorizationGrant.getIdToken() != null) {
                    indexByIdToken(authorizationGrant.getIdToken(), authorizationGrant);
                }
                for (AccessToken accessToken : authorizationGrant.getAccessTokens()) {
                    indexByAccessToken(accessToken, authorizationGrant);
                }
                for (RefreshToken refreshToken : authorizationGrant.getRefreshTokens()) {
                    indexByRefreshToken(refreshToken, authorizationGrant);
                }
                if (authorizationGrant.getClient() != null) {
                    indexByClient(authorizationGrant.getClient(), authorizationGrant);
                }
            }
        }
    }

    /**
     * Creates an {@link AuthorizationGrant}
     *
     * @param user               The resource owner.
     * @param client             An application making protected resource requests on behalf of the resource owner and
     *                           with its authorization.
     * @param authenticationTime The Claim Value is the number of seconds from 1970-01-01T0:0:0Z as measured in UTC
     *                           until the date/time that the End-User authentication occurred.
     * @return The authorization grant.
     */
    @Override
    public AuthorizationGrant createAuthorizationGrant(User user, Client client, Date authenticationTime) {
        AuthorizationGrant authorizationGrant = new AuthorizationGrant(user, null, client, authenticationTime);

        addAuthorizationGrant(authorizationGrant);

        LOGGER.debug(
                "Created new AuthorizationGrant for User: {0}, Client: {1}",
                user.getUserId(),
                authorizationGrant.getClient().getClientId());

        return authorizationGrant;
    }

    /**
     * Creates an {@link AuthorizationCodeGrant}.
     *
     * @param user               The resource owner.
     * @param client             An application making protected resource requests on behalf of
     *                           the resource owner and with its authorization.
     * @param authenticationTime The Claim Value is the number of seconds from 1970-01-01T0:0:0Z as measured in UTC
     *                           until the date/time that the End-User authentication occurred.
     * @return The authorization code grant.
     */
    @Override
    public AuthorizationCodeGrant createAuthorizationCodeGrant(User user, Client client, Date authenticationTime) {
        AuthorizationCodeGrant authorizationCodeGrant = new AuthorizationCodeGrant(user, client, authenticationTime);

        addAuthorizationGrant(authorizationCodeGrant);

        LOGGER.debug(
                "Created new AuthorizationCodeGrant for User: {0}, Client: {1} and AuthorizationCode: {2}",
                user.getUserId(),
                authorizationCodeGrant.getClient().getClientId(),
                authorizationCodeGrant.getAuthorizationCode().getCode());

        return authorizationCodeGrant;
    }

    /**
     * Creates an {@link ImplicitGrant}.
     *
     * @param user               The resource owner.
     * @param client             An application making protected resource requests on behalf of the resource owner and
     *                           with its authorization.
     * @param authenticationTime The Claim Value is the number of seconds from 1970-01-01T0:0:0Z as measured in UTC
     *                           until the date/time that the End-User authentication occurred.
     * @return The implicit grant
     */
    @Override
    public ImplicitGrant createImplicitGrant(User user, Client client, Date authenticationTime) {
        ImplicitGrant implicitGrant = new ImplicitGrant(user, client, authenticationTime);

        addAuthorizationGrant(implicitGrant);

        LOGGER.debug("Created new ImplicitGrant for User {0} and Client: {1}", user,
                implicitGrant.getClient().getClientId());

        return implicitGrant;
    }

    /**
     * Creates a {@link ClientCredentialsGrant}.
     *
     * @param user   The resource owner.
     * @param client An application making protected resource requests on behalf of
     *               the resource owner and with its authorization.
     * @return The client credentials grant.
     */
    @Override
    public ClientCredentialsGrant createClientCredentialsGrant(User user, Client client) {
        ClientCredentialsGrant clientCredentialsGrant = new ClientCredentialsGrant(user, client);

        addAuthorizationGrant(clientCredentialsGrant);

        LOGGER.debug("Created new ClientCredentialsGrant for User {0} and Client: {1}", user,
                clientCredentialsGrant.getClient().getClientId());

        return clientCredentialsGrant;
    }

    /**
     * Creates a {@link ResourceOwnerPasswordCredentialsGrant}
     *
     * @param user   The resource owner.
     * @param client An application making protected resource requests on behalf of
     *               the resource owner and with its authorization.
     * @return The resource owner password credentials grant.
     */
    @Override
    public ResourceOwnerPasswordCredentialsGrant createResourceOwnerPasswordCredentialsGrant(User user, Client client) {
        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant =
                new ResourceOwnerPasswordCredentialsGrant(user, client);

        addAuthorizationGrant(resourceOwnerPasswordCredentialsGrant);

        LOGGER.debug(
                "Created new ResourceOwnerPasswordCredentialsGrant for User: {0} and Client: {0}",
                user.getUserId(),
                client.getClientId());

        return resourceOwnerPasswordCredentialsGrant;
    }

    /**
     * Search the authorization grant given an authorization code.
     * <p/>
     * The client MUST NOT use the authorization code more than once. If an
     * authorization code is used more than once, the authorization server MUST
     * deny the request and SHOULD attempt to revoke all tokens previously
     * issued based on that authorization code.
     *
     * @param clientId          An application making protected resource requests on behalf of
     *                          the resource owner and with its authorization.
     * @param authorizationCode
     * @return The authorization code grant, otherwise <code>null</code>.
     */
    @Override
    public AuthorizationCodeGrant getAuthorizationCodeGrant(String clientId, String authorizationCode) {
        LOGGER.debug("Looking for an authorization code grant for client: {0} Authorization code: {1}",
                clientId, authorizationCode);

        AuthorizationGrant authorizationGrant = authorizationGrantsByCode.get(authorizationCode);

        if (authorizationGrant != null && authorizationGrant instanceof AuthorizationCodeGrant) {
            AuthorizationCodeGrant authorizationCodeGrant = (AuthorizationCodeGrant) authorizationGrant;
            if (authorizationCodeGrant.getClient().getClientId().equals(clientId)) {
                if (authorizationCodeGrant.getAuthorizationCode().isValid()) {
                    LOGGER.debug("Authorization code grant found");
                    return authorizationCodeGrant;
                } else {
                    LOGGER.debug("Revoking all tokens");
                    authorizationCodeGrant.revokeAllTokens();
                }
            }
        }

        LOGGER.debug("Authorization code grant not found");
        return null;
    }

    /**
     * Search the authorization grant given a refresh token. The refresh token
     * must be valid, otherwise the function will return null.
     *
     * @param clientId         An application making protected resource requests on behalf of
     *                         the resource owner and with its authorization.
     * @param refreshTokenCode The refresh token code.
     * @return The authorization grant, otherwise <code>null</code>.
     */
    @Override
    public AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode) {
        LOGGER.debug("Looking for an authorization grant for client: {0} Refresh token: {1}",
                clientId, refreshTokenCode);

        AuthorizationGrant authorizationGrant = authorizationGrantsByRefreshToken.get(refreshTokenCode);

        if (authorizationGrant != null) {
            if (authorizationGrant.getClient().getClientId().equals(clientId)) {
                RefreshToken refreshToken = authorizationGrant.getRefreshToken(refreshTokenCode);
                if (refreshToken != null && refreshToken.isValid()) {
                    LOGGER.debug("Authorization grant found");
                    return authorizationGrant;
                } else if (refreshToken != null && refreshToken.isRevoked()) {
                    // Inform the authorization server of a possible attack.
                    LOGGER.warn("Attempt to use a revoked refresh token");
                }
            }
        }

        LOGGER.debug("Authorization grant not found");
        return null;
    }

    @Override
    public List<AuthorizationGrant> getAuthorizationGrant(String clientId) {
        LOGGER.debug("Looking for authorization grants for client: {0}", clientId);

        List<AuthorizationGrant> authorizationGrantList = new ArrayList<AuthorizationGrant>(authorizationGrantsByClientId.get(clientId));
        LOGGER.debug("Authorization grants found: {0}", authorizationGrantList.size());

        return authorizationGrantList;
    }

    /**
     * Search the authorization grant given an access token.
     *
     * @param tokenCode The access token code.
     * @return The authorization grant, otherwise <code>null</code>.
     */
    @Override
    public AuthorizationGrant getAuthorizationGrantByAccessToken(String tokenCode) {
        LOGGER.debug("Looking for an authorization grant for the token: {0}", tokenCode);

        AuthorizationGrant authorizationGrant = authorizationGrantsByAccessToken.get(tokenCode);

        if (authorizationGrant != null) {
            AbstractToken token = authorizationGrant.getAccessToken(tokenCode);
            if (token != null && token.isValid()) {
                LOGGER.debug("Authorization grant found");
                return authorizationGrant;
            }
        }

        return null;
    }

    @Override
    public AuthorizationGrant getAuthorizationGrantByIdToken(String idToken) {
        LOGGER.debug("Looking for an authorization grant fir id token: {0}", idToken);

        AuthorizationGrant authorizationGrant = authorizationGrantsByIdToken.get(idToken);

        if (authorizationGrant != null) {
            if (authorizationGrant.getIdToken() != null && authorizationGrant.getIdToken().isValid()) {
                LOGGER.debug("Authorization grant found");
                return authorizationGrant;
            }
        }

        return null;
    }

    @Override
    public synchronized void indexByAuthorizationCode(AuthorizationCode authorizationCode, AuthorizationGrant authorizationGrant) {
        if (authorizationCode != null && authorizationGrant != null) {
            authorizationGrantsByCode.put(authorizationCode.getCode(), authorizationGrant);
        }
    }

    @Override
    public synchronized void indexByAccessToken(AccessToken accessToken, AuthorizationGrant authorizationGrant) {
        if (accessToken != null && authorizationGrant != null) {
            authorizationGrantsByAccessToken.put(accessToken.getCode(), authorizationGrant);
        }
    }

    @Override
    public synchronized void indexByRefreshToken(RefreshToken refreshToken, AuthorizationGrant authorizationGrant) {
        if (refreshToken != null && authorizationGrant != null) {
            authorizationGrantsByRefreshToken.put(refreshToken.getCode(), authorizationGrant);
        }
    }

    @Override
    public synchronized void indexByIdToken(IdToken idToken, AuthorizationGrant authorizationGrant) {
        if (idToken != null && authorizationGrant != null) {
            authorizationGrantsByIdToken.put(idToken.getCode(), authorizationGrant);
        }
    }

    @Override
    public synchronized void indexByClient(Client client, AuthorizationGrant authorizationGrant) {
        if (client != null && authorizationGrant != null) {
            authorizationGrantsByClientId.put(client.getClientId(), authorizationGrant);
        }
    }
}