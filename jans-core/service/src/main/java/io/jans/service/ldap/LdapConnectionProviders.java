/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
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
