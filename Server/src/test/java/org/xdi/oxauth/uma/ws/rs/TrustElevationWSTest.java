package org.xdi.oxauth.uma.ws.rs;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.*;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.net.URI;
import java.util.Arrays;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class TrustElevationWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token pat;
	private static RPTResponse rpt;
	private static UmaResourceResponse resource;
	private static PermissionTicket ticket;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaRedirectUri", "umaRptPath", "umaRegisterResourcePath" })
	public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret,
			String umaRedirectUri, String umaRptPath, String umaRegisterResourcePath) {
		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);

		rpt = TUma.requestRpt(url, umaRptPath);

		UmaTestUtil.assert_(pat);
		UmaTestUtil.assert_(rpt);

		resource = TUma.registerResource(url, pat, umaRegisterResourcePath, UmaTestUtil.createResource());
		UmaTestUtil.assert_(resource);
	}

	@Test(dependsOnMethods = { "init" })
	@Parameters({"umaPermissionPath"})
	public void registerPermissionForRpt(String umaPermissionPath)
			throws Exception {
		final UmaPermission r = new UmaPermission();
		r.setResourceId(resource.getId());
		r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

		ticket = TUma.registerPermission(url, pat, r, umaPermissionPath);
		UmaTestUtil.assert_(ticket);
	}

	@Test(dependsOnMethods = { "registerPermissionForRpt" })
	@Parameters({ "umaPermissionAuthorizationPath"})
	public void authorizePermission(String umaPermissionAuthorizationPath) {
		final RptAuthorizationRequest request = new RptAuthorizationRequest();
		request.setRpt(rpt.getRpt());
		request.setTicket(ticket.getTicket());
		request.setClaims(new ClaimTokenList().addToken(new ClaimToken("clientClaim", "clientValue")));

		// todo uma2
//		final RptAuthorizationResponse response = TUma.requestAuthorization(url, umaPermissionAuthorizationPath,
//				aat, request);
//		assertNotNull(response, "Token response status is null");

		// final RptIntrospectionResponse status = TUma.requestRptStatus(this,
		// umaRptStatusPath, pat, m_rpt.getRpt());
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
