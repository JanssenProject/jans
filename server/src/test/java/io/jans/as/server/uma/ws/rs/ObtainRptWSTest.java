/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.server.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

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
