/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import java.net.URI;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.uma.TUma;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.uma.UmaTestUtil;

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
