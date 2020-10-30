/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.server.BaseTest;
import io.jans.as.common.service.common.EncryptionService;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.inject.Inject;

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