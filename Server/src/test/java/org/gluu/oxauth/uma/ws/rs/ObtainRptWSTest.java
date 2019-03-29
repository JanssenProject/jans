/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import java.net.URI;

import org.gluu.oxauth.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainRptWSTest extends BaseTest {

	@ArquillianResource
	private URI url;


	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret",
			"umaRedirectUri" })
	public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaRedirectUri) {
	}

	@Test(dependsOnMethods = "init")
	@Parameters({ "umaRptPath"})
	public void obtainRpt(String umaRptPath) {
		// todo uma2
//		final RPTResponse r = TUma.requestRpt(url, aat, umaRptPath);
//		UmaTestUtil.assert_(r);
	}
}
