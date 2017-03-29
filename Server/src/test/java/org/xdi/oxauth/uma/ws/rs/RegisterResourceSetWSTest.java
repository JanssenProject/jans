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

import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterResourceSetWSTest extends BaseTest {

	@ArquillianResource
    private URI url;

    private static Token pat;
    private static ResourceSetResponse resourceSetStatus;
    private static String umaRegisterResourcePath;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri", "umaRegisterResourcePath"})
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
        pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        this.umaRegisterResourcePath = umaRegisterResourcePath;
    }

    @Test(dependsOnMethods = {"init"})
    public void testRegisterResourceSet() throws Exception {
        resourceSetStatus = TUma.registerResourceSet(url, pat, umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(resourceSetStatus);
    }

    @Test(dependsOnMethods = {"testRegisterResourceSet"})
    public void testModifyResourceSet() throws Exception {
        final ResourceSet resourceSet = new ResourceSet();
        resourceSet.setName("Server Photo Album 2");
        resourceSet.setIconUri("http://www.example.com/icons/flower.png");
        resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

        final ResourceSetResponse status = TUma.modifyResourceSet(url, pat, umaRegisterResourcePath, resourceSetStatus.getId(), resourceSet);
        UmaTestUtil.assert_(status);
    }

    /**
     * Test for getting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testGetResourceSets() throws Exception {
        final List<String> list = TUma.getResourceSetList(url, pat, umaRegisterResourcePath);

        assertTrue(list != null && !list.isEmpty() && list.contains(resourceSetStatus.getId()), "Resource set list is empty");
    }


    /**
     * Test for deleting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testGetResourceSets"})
    public void testDeleteResourceSet() throws Exception {
        TUma.deleteResourceSet(url, pat, umaRegisterResourcePath, resourceSetStatus.getId());
    }
}
