/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.operation.impl;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.ResultCode;

/**
 * Authentication connection provider
 *
 * @author Yuriy Movchan Date: 12/29/2017
 */
public class LdapAuthConnectionProvider extends LdapConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(LdapAuthConnectionProvider.class);

    public LdapAuthConnectionProvider(Properties connectionProperties) {
        Properties bindConnectionProperties = prepareBindConnectionProperties(connectionProperties);
        create(bindConnectionProperties);
        if (ResultCode.INAPPROPRIATE_AUTHENTICATION.equals(getCreationResultCode())) {
            LOG.warn("It's not possible to create authentication LDAP connection pool using anonymous bind. "
                    + "Attempting to create it using binDN/bindPassword");
            create(connectionProperties);
        }
    }

    private Properties prepareBindConnectionProperties(Properties connectionProperties) {
        Properties bindProperties = (Properties) connectionProperties.clone();
        bindProperties.remove("bindDN");
        bindProperties.remove("bindPassword");

        return bindProperties;
    }

}
