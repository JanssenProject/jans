package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;

/**
 * @author Yuriy Z
 */
public class TxTokenGrant extends AuthorizationGrant {

    public TxTokenGrant() {
    }

    /**
     * Construct a tx token grant.
     *
     * @param user   The resource owner.
     * @param client An application making protected resource requests on behalf of
     *               the resource owner and with its authorization.
     */
    public TxTokenGrant(User user, Client client) {
        init(user, client);
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.TX_TOKEN;
    }

    public void init(User user, Client client) {
        super.init(user, AuthorizationGrantType.TX_TOKEN, client, null);
    }

    /**
     * The authorization server MUST NOT issue a refresh token.
     */
    @Override
    public RefreshToken createRefreshToken(ExecutionContext executionContext) {
        throw new UnsupportedOperationException(
                "The authorization server MUST NOT issue a refresh token.");
    }
}
