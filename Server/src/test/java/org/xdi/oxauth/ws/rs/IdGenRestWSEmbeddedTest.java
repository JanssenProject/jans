/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.common.Holder;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */

public class IdGenRestWSEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token m_aat;
	private static RPTResponse m_rpt;
	private static final Holder<PermissionTicket> m_ticketH = new Holder<PermissionTicket>();

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret",
			"umaRedirectUri" })
	public void init_0(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaAatClientId, String umaAatClientSecret, String umaRedirectUri) {
		m_aat = TUma.requestAat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId,
				umaAatClientSecret, umaRedirectUri);
		UmaTestUtil.assert_(m_aat);
	}

	@Test(dependsOnMethods = { "init_0" })
	@Parameters({ "umaRptPath", "umaAmHost" })
	public void init(String umaRptPath, String umaAmHost) {
		m_rpt = TUma.requestRpt(url, m_aat, umaRptPath, umaAmHost);
		UmaTestUtil.assert_(m_rpt);
	}

	@Test(dependsOnMethods = { "init" })
	@Parameters({ "idGenerationPath" })
	public void requestInumForOpenidConnectClient_Negative(String idGenerationPath) throws Exception {
		final String prefix = "@!1111";
		final String path = idGenerationPath + "/" + prefix + "/" + IdType.CLIENTS.getType();

		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer " + m_rpt.getRpt());

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestInumForOpenidConnectClient", response, entity);

		assertEquals(response.getStatus(), 403); // forbidden
		try {
			final PermissionTicket t = ServerUtil.createJsonMapper().readValue(entity, PermissionTicket.class);
			UmaTestUtil.assert_(t);
			m_ticketH.setT(t);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test(dependsOnMethods = { "requestInumForOpenidConnectClient_Negative" })
	@Parameters({ "umaPermissionAuthorizationPath", "umaAmHost" })
	public void authorizeRpt(String umaPermissionAuthorizationPath, String umaAmHost) {
		final RptAuthorizationRequest request = new RptAuthorizationRequest();
		request.setRpt(m_rpt.getRpt());
		request.setTicket(m_ticketH.getT().getTicket());

		final RptAuthorizationResponse response = TUma.requestAuthorization(url, umaPermissionAuthorizationPath,
				umaAmHost, m_aat, request);
		assertNotNull(response, "Token response status is null");
	}

	@Test(dependsOnMethods = { "authorizeRpt" })
	@Parameters({ "idGenerationPath" })
	public void requestInumForOpenidConnectClient(String idGenerationPath) throws Exception {
		final String prefix = "@!1111";
		final String path = idGenerationPath + "/" + prefix + "/" + IdType.CLIENTS.getType();

		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer " + m_rpt.getRpt());

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestInumForOpenidConnectClient", response, entity);

		assertEquals(response.getStatus(), 200); // OK
		try {
			final Id id = ServerUtil.createJsonMapper().readValue(entity, Id.class);
			UmaTestUtil.assert_(id);
			assertTrue(id.getId().startsWith(prefix));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test(dependsOnMethods = { "requestInumForOpenidConnectClient" })
	@Parameters({ "idGenerationPath" })
	public void requestPeopleInum(String idGenerationPath) throws Exception {
		final String prefix = "@!1111";
		final String path = idGenerationPath + "/" + prefix + "/" + IdType.PEOPLE;

		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
		request.header("Accept", "text/plain");
		request.header("Authorization", "Bearer " + m_rpt.getRpt());

		Response response = request.get();
		String entity = response.readEntity(String.class);

		showResponse("requestPeopleInum", response, entity);

		final String id = entity;

		assertEquals(response.getStatus(), 200, "Unexpected response code.");
		assertTrue(id.startsWith(prefix), "Unexpected id.");
	}

}
