package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;

/**
 * @author Yuriy Z
 */
public class TokenExchangeGrant extends AuthorizationGrant {

    public void init(User user, Client client) {
        super.init(user, AuthorizationGrantType.TOKEN_EXCHANGE, client, null);
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.TOKEN_EXCHANGE;
    }
}
