/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetStatus;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterResourceSetWSTest extends BaseTest {

    private Token m_pat;
    private ResourceSetStatus m_resourceSetStatus;
    private String m_umaRegisterResourcePath;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri", "umaRegisterResourcePath"})
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
        m_pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        m_umaRegisterResourcePath = umaRegisterResourcePath;
    }

    @Test(dependsOnMethods = {"init"})
    public void testRegisterResourceSet() throws Exception {
        m_resourceSetStatus = TUma.registerResourceSet(this, m_pat, m_umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(m_resourceSetStatus);
    }

    @Test(dependsOnMethods = {"testRegisterResourceSet"})
    public void testModifyResourceSet() throws Exception {
        final ResourceSet resourceSet = new ResourceSet();
        resourceSet.setName("Server Photo Album 2");
        resourceSet.setIconUri("http://www.example.com/icons/flower.png");
        resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

        final ResourceSetStatus status = TUma.modifyResourceSet(this, m_pat, m_umaRegisterResourcePath, m_resourceSetStatus.getId(), resourceSet);
        UmaTestUtil.assert_(status);
    }

    /**
     * Test for getting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testGetResourceSets() throws Exception {
        final List<String> list = TUma.getResourceSetList(this, m_pat, m_umaRegisterResourcePath);

        assertTrue(list != null && !list.isEmpty() && list.contains(m_resourceSetStatus.getId()), "Resource set list is empty");
    }


    /**
     * Test for deleting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testGetResourceSets"})
    public void testDeleteResourceSet() throws Exception {
        TUma.deleteResourceSet(this, m_pat, m_umaRegisterResourcePath, m_resourceSetStatus.getId());
    }
}
