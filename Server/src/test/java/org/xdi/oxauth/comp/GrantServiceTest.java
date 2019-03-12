/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.ldap.TokenLdap;
import org.xdi.oxauth.model.ldap.TokenType;
import org.xdi.oxauth.service.GrantService;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version September 16, 2015
 */

public class GrantServiceTest extends BaseComponentTest {

	private static final String TEST_TOKEN_CODE = UUID.randomUUID().toString();

	@Inject
	private GrantService grantService;

	private static String m_clientId;

	private static TokenLdap m_tokenLdap;

	@Parameters(value = "clientId")
	@Test
	public void createTestToken(String clientId) {
		this.m_clientId = clientId;
		m_tokenLdap = createTestToken();
		grantService.persist(m_tokenLdap);
	}

	@Test(dependsOnMethods = "createTestToken")
	public void testCleanUp() {
		grantService.cleanUp(); // clean up must remove just created token
								// because expiration is set to new Date()
		final TokenLdap t = grantService.getGrantsByCode(TEST_TOKEN_CODE);
		Assert.assertTrue(t == null);
	}

	@Test(dependsOnMethods = "createTestToken")
	public void removeTestTokens() {
		final TokenLdap t = grantService.getGrantsByCode(TEST_TOKEN_CODE);
		if (t != null) {
			grantService.remove(t);
		}
	}

	private TokenLdap createTestToken() {
		final String id = GrantService.generateGrantId();
		final String grantId = GrantService.generateGrantId();
		final String dn = grantService.buildDn(id, m_clientId);

		final TokenLdap t = new TokenLdap();
		t.setId(id);
		t.setDn(dn);
		t.setGrantId(grantId);
		t.setClientId(m_clientId);
		t.setTokenCode(TEST_TOKEN_CODE);
		t.setTokenType(TokenType.ACCESS_TOKEN.getValue());
		t.setCreationDate(new Date());
		t.setExpirationDate(new Date());
		return t;
	}

}
