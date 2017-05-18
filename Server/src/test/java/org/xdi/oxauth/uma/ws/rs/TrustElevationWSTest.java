package org.xdi.oxauth.uma.ws.rs;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.*;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.net.URI;
import java.util.Arrays;

import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class TrustElevationWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token pat;
	private static Token aat;
	private static RPTResponse rpt;
	private static UmaResourceResponse resource;
	private static PermissionTicket ticket;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaAatClientId", "umaAatClientSecret", "umaRedirectUri", "umaRptPath", "umaAmHost",
			"umaRegisterResourcePath" })
	public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret, String umaAatClientId, String umaAatClientSecret,
			String umaRedirectUri, String umaRptPath, String umaAmHost, String umaRegisterResourcePath) {
		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);
		aat = TUma.requestAat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId,
				umaAatClientSecret, umaRedirectUri);

		rpt = TUma.requestRpt(url, aat, umaRptPath, umaAmHost);

		UmaTestUtil.assert_(pat);
		UmaTestUtil.assert_(aat);
		UmaTestUtil.assert_(rpt);

		resource = TUma.registerResource(url, pat, umaRegisterResourcePath, UmaTestUtil.createResource());
		UmaTestUtil.assert_(resource);
	}

	@Test(dependsOnMethods = { "init" })
	@Parameters({ "umaAmHost", "umaHost", "umaPermissionPath" })
	public void registerPermissionForRpt(final String umaAmHost, String umaHost, String umaPermissionPath)
			throws Exception {
		final UmaPermission r = new UmaPermission();
		r.setResourceId(resource.getId());
		r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

		ticket = TUma.registerPermission(url, pat, umaAmHost, umaHost, r, umaPermissionPath);
		UmaTestUtil.assert_(ticket);
	}

	@Test(dependsOnMethods = { "registerPermissionForRpt" })
	@Parameters({ "umaPermissionAuthorizationPath", "umaAmHost" })
	public void authorizePermission(String umaPermissionAuthorizationPath, String umaAmHost) {
		final RptAuthorizationRequest request = new RptAuthorizationRequest();
		request.setRpt(rpt.getRpt());
		request.setTicket(ticket.getTicket());
		request.setClaims(new ClaimTokenList().addToken(new ClaimToken("clientClaim", "clientValue")));

		final RptAuthorizationResponse response = TUma.requestAuthorization(url, umaPermissionAuthorizationPath,
				umaAmHost, aat, request);
		assertNotNull(response, "Token response status is null");

		// final RptIntrospectionResponse status = TUma.requestRptStatus(this,
		// umaRptStatusPath, umaAmHost, m_pat, m_rpt.getRpt());
		// UmaTestUtil.assert_(status);
	}

	// use normal test method instead of @AfterClass because it will not work
	// with ResourceRequestEnvironment seam class which is used
	// behind TUma wrapper.
	@Test(dependsOnMethods = { "authorizePermission" })
	@Parameters({ "umaRegisterResourcePath" })
	public void cleanUp(String umaRegisterResourcePath) {
		if (resource != null) {
			TUma.deleteResource(url, pat, umaRegisterResourcePath, resource.getId());
		}
	}

}
