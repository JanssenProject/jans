/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.server.BaseComponentTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Javier Rojas Blum Date: 05.30.2012
 */
public class KeyGenerationTest extends BaseComponentTest {

    @Parameters({"ldapAdminPassword"})
    @Test
    public void encryptLdapPassword(final String ldapAdminPassword) throws Exception {
        showTitle("TEST: encryptLdapPassword");

        String password = getEncryptionService().encrypt(ldapAdminPassword);
        System.out.println("Encrypted LDAP Password: " + password);
    }

}