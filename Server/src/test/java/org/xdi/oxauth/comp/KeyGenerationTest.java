/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import javax.inject.Inject;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.service.EncryptionService;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Javier Rojas Blum Date: 05.30.2012
 */
public class KeyGenerationTest extends BaseTest {

	@Inject
	private EncryptionService encryptionService;

	@Parameters({ "ldapAdminPassword" })
	@Test
	public void encryptLdapPassword(final String ldapAdminPassword) throws Exception {
		showTitle("TEST: encryptLdapPassword");

		String password = encryptionService.encrypt(ldapAdminPassword);
		System.out.println("Encrypted LDAP Password: " + password);
	}

}