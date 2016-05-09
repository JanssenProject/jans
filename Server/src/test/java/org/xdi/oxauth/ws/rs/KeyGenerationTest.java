/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.service.EncryptionService;

/**
 * @author Javier Rojas Blum Date: 05.30.2012
 */
public class KeyGenerationTest extends BaseTest {

    @Parameters({"ldapAdminPassword"})
    @Test
    public void encryptLdapPassword(final String ldapAdminPassword) throws Exception {
    	new ComponentTest() {
			@Override
			protected void testComponents() throws Exception {
		        showTitle("TEST: encryptLdapPassword");

		        String password = EncryptionService.instance().encrypt(ldapAdminPassword);
		        System.out.println("Encrypted LDAP Password: " + password);
			}
    	}.run();
    }
}