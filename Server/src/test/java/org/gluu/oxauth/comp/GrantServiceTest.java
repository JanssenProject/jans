/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.comp;

import org.gluu.oxauth.BaseComponentTest;
import org.gluu.oxauth.model.ldap.TokenLdap;
import org.gluu.oxauth.model.ldap.TokenType;
import org.gluu.oxauth.service.GrantService;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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
