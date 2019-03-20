/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.common.AuthorizationGrantList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/01/2013
 */

public class UtilityMethodsTest extends BaseComponentTest {

	@Inject
	private AuthorizationGrantList authorizationGrantList;

	@Test
	public void test() {
		final String dn = "oxAuthTokenCode=d76e48fe-c9f9-4d82-871c-b097f6c52875,inum=@!1111!0008!FF81!2D39,ou=clients,o=gluu";
		final String clientId = authorizationGrantList.extractClientIdFromTokenDn(dn);
		Assert.assertTrue(clientId.equals("@!1111!0008!FF81!2D39"));
	}

}
