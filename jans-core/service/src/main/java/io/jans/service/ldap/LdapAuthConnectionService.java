/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.ldap;

import java.util.Properties;

import io.jans.orm.ldap.operation.impl.LdapAuthConnectionProvider;

/**
 * Super class to forbid interceptor calls
 *
 * @author Yuriy Movchan Date: 12/29/2017
 */
public class LdapAuthConnectionService extends LdapAuthConnectionProvider {

    public LdapAuthConnectionService(Properties props) {
        super(props);
    }

}
