/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.server.BaseTest;
import io.jans.as.model.uma.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.model.uma.TUma;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainPatWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token pat;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaRedirectUri" })
	public void requestPat(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);
		UmaTestUtil.assert_(pat);
	}

	@Test(dependsOnMethods = "requestPat")
	@Parameters({ "tokenPath", "umaPatClientId", "umaPatClientSecret" })
	public void requestNewPatByRefreshTokne(String tokenPath, String umaPatClientId, String umaPatClientSecret) {
		final Token newPat = TUma.newTokenByRefreshToken(url, tokenPath, pat, umaPatClientId, umaPatClientSecret);
		UmaTestUtil.assert_(newPat);
	}
}
