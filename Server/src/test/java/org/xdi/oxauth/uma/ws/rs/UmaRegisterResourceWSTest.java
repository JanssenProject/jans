/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.uma.UmaResource;
import org.gluu.oxauth.model.uma.UmaResourceResponse;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class UmaRegisterResourceWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token pat;
	private static UmaResourceResponse resourceStatus;
	private static String umaRegisterResourcePath;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaRedirectUri", "umaRegisterResourcePath" })
	public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);
		this.umaRegisterResourcePath = umaRegisterResourcePath;
	}

	@Test(dependsOnMethods = { "init" })
	public void testRegisterResource() throws Exception {
		resourceStatus = TUma.registerResource(url, pat, umaRegisterResourcePath,
				UmaTestUtil.createResource());
		UmaTestUtil.assert_(resourceStatus);
	}

	@Test(dependsOnMethods = {"testRegisterResource"})
	public void testModifyResource() throws Exception {
		final UmaResource resource = new UmaResource();
		resource.setName("Server Photo Album 2");
		resource.setIconUri("http://www.example.com/icons/flower.png");
		resource.setScopes(
				Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

		final UmaResourceResponse status = TUma.modifyResource(url, pat, umaRegisterResourcePath,
				resourceStatus.getId(), resource);
		UmaTestUtil.assert_(status);
	}

	/**
	 * Test for getting UMA resource descriptions
	 */
	@Test(dependsOnMethods = {"testModifyResource"})
	public void testGetResources() throws Exception {
		final List<String> list = TUma.getResourceList(url, pat, umaRegisterResourcePath);

		assertTrue(list != null && !list.isEmpty() && list.contains(resourceStatus.getId()),
				"Resource list is empty");
	}

	/**
	 * Test for deleting UMA resource descriptions
	 */
	@Test(dependsOnMethods = {"testGetResources"})
	public void testDeleteResource() throws Exception {
		TUma.deleteResource(url, pat, umaRegisterResourcePath, resourceStatus.getId());
	}
}
