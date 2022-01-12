/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.ldap;

import java.util.Properties;

import io.jans.orm.ldap.operation.impl.LdapConnectionProvider;

/**
 * Super class to forbid interceptor calls
 *
 * @author Yuriy Movchan Date: 08/09/2013
 */
public class LdapConnectionService extends LdapConnectionProvider {

    public LdapConnectionService(Properties props) {
        super(props);
    }

}
