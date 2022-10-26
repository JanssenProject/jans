/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.model.ldap.TokenType;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.util.TokenHashUtil;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version September 16, 2015
 */

public class GrantServiceTest extends BaseComponentTest {

    private static final String TEST_TOKEN_CODE = UUID.randomUUID().toString();

    private static String clientId;

    private static TokenEntity tokenEntity;

    @Parameters(value = "clientId")
    @Test
    public void createTestToken(String clientId) {
        GrantServiceTest.clientId = clientId;
        tokenEntity = createTestToken();
        getGrantService().persist(tokenEntity);
    }

    @Test(dependsOnMethods = "createTestToken")
    public void removeTestTokens() {
        final TokenEntity t = getGrantService().getGrantByCode(TEST_TOKEN_CODE);
        if (t != null) {
            getGrantService().remove(t);
        }
    }

    private TokenEntity createTestToken() {
        final String grantId = GrantService.generateGrantId();
        final String dn = getGrantService().buildDn(TokenHashUtil.hash(TEST_TOKEN_CODE));

        final TokenEntity t = new TokenEntity();
        t.setDn(dn);
        t.setGrantId(grantId);
        t.setClientId(clientId);
        t.setTokenCode(TokenHashUtil.hash(TEST_TOKEN_CODE));
        t.setTokenType(TokenType.ACCESS_TOKEN.getValue());
        t.setCreationDate(new Date());
        t.setExpirationDate(new Date());
        return t;
    }

}
