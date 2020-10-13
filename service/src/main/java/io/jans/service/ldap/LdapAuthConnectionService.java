/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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
