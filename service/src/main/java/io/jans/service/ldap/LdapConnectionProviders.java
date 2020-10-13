/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.ldap;

public class LdapConnectionProviders {
    private LdapConnectionService connectionProvider;
    private LdapConnectionService connectionBindProvider;

    public LdapConnectionProviders(LdapConnectionService connectionProvider, LdapConnectionService connectionBindProvider) {
        this.connectionProvider = connectionProvider;
        this.connectionBindProvider = connectionBindProvider;
    }

    public LdapConnectionService getConnectionProvider() {
        return connectionProvider;
    }

    public LdapConnectionService getConnectionBindProvider() {
        return connectionBindProvider;
    }
}
