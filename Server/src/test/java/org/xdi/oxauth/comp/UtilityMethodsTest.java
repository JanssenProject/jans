/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import org.gluu.oxauth.model.common.AuthorizationGrantList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/01/2013
 */

public class UtilityMethodsTest {

	@Test
	public void test() {
		final String dn = "uniqueIdentifier=fe6c2e7b-8c54-41d1-8cd1-c816371525dc,ou=token,inum=aa94b930-956a-4e3d-a150-d13735c3712b,ou=clients,o=gluu";
		final String clientId = AuthorizationGrantList.extractClientIdFromTokenDn(dn);
		assertEquals("aa94b930-956a-4e3d-a150-d13735c3712b", clientId);
	}

}
