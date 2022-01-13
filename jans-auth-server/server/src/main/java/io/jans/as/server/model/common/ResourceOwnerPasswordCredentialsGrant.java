/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;

/**
 * <p>
 * The resource owner password credentials (i.e. username and password) can be
 * used directly as an authorization grant to obtain an access token. The
 * credentials should only be used when there is a high degree of trust between
 * the resource owner and the client (e.g. its device operating system or a
 * highly privileged application), and when other authorization grant types are
 * not available (such as an authorization code).
 * </p>
 * <p>
 * Even though this grant type requires direct client access to the resource
 * owner credentials, the resource owner credentials are used for a single
 * request and are exchanged for an access token. This grant type can eliminate
 * the need for the client to store the resource owner credentials for future
 * use, by exchanging the credentials with a long-lived access token or refresh
 * token.
 * </p>
 *
 * @author Javier Rojas Blum Date: 09.29.2011
 * @author Yuriy Movchan
 */
public class ResourceOwnerPasswordCredentialsGrant extends AuthorizationGrant {

    public ResourceOwnerPasswordCredentialsGrant() {
    }

    /**
     * Constructs a resource owner password credentials grant.
     *
     * @param user   The resource owner.
     * @param client An application making protected resource requests on behalf of
     *               the resource owner and with its authorization.
     */
    public ResourceOwnerPasswordCredentialsGrant(User user, Client client) {
        init(user, client);
    }

    public void init(User user, Client client) {
        super.init(user, AuthorizationGrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS, client, null);
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS;
    }

}