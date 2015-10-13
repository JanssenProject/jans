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

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version September 16, 2015
 */

public class GrantServiceTest extends BaseComponentTest {

    private static final String TEST_TOKEN_CODE = UUID.randomUUID().toString();

    private String m_clientId;
    private GrantService m_grantService;
    private TokenLdap m_tokenLdap;

    @Parameters(value = "clientId")
    public GrantServiceTest(String p_clientId) {
        m_clientId = p_clientId;
    }

    @Override
    public void beforeClass() {
        m_grantService = GrantService.instance();
        m_tokenLdap = createTestToken();
        m_grantService.persist(m_tokenLdap);
    }

    @Override
    public void afterClass() {
        final TokenLdap t = m_grantService.getGrantsByCode(TEST_TOKEN_CODE);
        if (t != null) {
            m_grantService.remove(t);
        }
    }

    @Test
    public void testCleanUp() {
        m_grantService.cleanUp(); // clean up must remove just created token because expiration is set to new Date()
        final TokenLdap t = m_grantService.getGrantsByCode(TEST_TOKEN_CODE);
        Assert.assertTrue(t == null);
    }

    private TokenLdap createTestToken() {
        final String id = GrantService.generateGrantId();
        final String grantId = GrantService.generateGrantId();
        final String dn = GrantService.buildDn(id, grantId, m_clientId);

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
