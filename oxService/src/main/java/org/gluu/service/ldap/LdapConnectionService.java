/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.service.ldap;

import java.util.Properties;

import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;

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
