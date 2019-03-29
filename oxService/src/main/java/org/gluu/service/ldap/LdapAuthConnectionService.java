/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service.ldap;

import java.util.Properties;

import org.gluu.persist.ldap.operation.impl.LdapAuthConnectionProvider;

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
