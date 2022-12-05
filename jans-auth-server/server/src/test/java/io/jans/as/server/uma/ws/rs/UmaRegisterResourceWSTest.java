/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.uma.UmaResource;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.uma.TUma;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertTrue;

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
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
            "umaRedirectUri", "umaRegisterResourcePath"})
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
        pat = TUma.requestPat(getApiTagetURI(url), authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
                umaPatClientSecret, umaRedirectUri);
        UmaRegisterResourceWSTest.umaRegisterResourcePath = umaRegisterResourcePath;
    }

    @Test(dependsOnMethods = {"init"})
    public void testRegisterResource() throws Exception {
        resourceStatus = TUma.registerResource(getApiTagetURI(url), pat, umaRegisterResourcePath,
                UmaTestUtil.createResource());
        UmaTestUtil.assertIt(resourceStatus);
    }

    @Test(dependsOnMethods = {"testRegisterResource"})
    public void testModifyResource() throws Exception {
        final UmaResource resource = new UmaResource();
        resource.setName("Server Photo Album 2");
        resource.setIconUri("http://www.example.com/icons/flower.png");
        resource.setScopes(
                Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

        final UmaResourceResponse status = TUma.modifyResource(getApiTagetURI(url), pat, umaRegisterResourcePath,
                resourceStatus.getId(), resource);
        UmaTestUtil.assertIt(status);
    }

    /**
     * Test for getting UMA resource descriptions
     */
    @Test(dependsOnMethods = {"testModifyResource"})
    public void testGetResources() throws Exception {
        final List<String> list = TUma.getResourceList(getApiTagetURI(url), pat, umaRegisterResourcePath);

        assertTrue(list != null && !list.isEmpty() && list.contains(resourceStatus.getId()),
                "Resource list is empty");
    }

    /**
     * Test for deleting UMA resource descriptions
     */
    @Test(dependsOnMethods = {"testGetResources"})
    public void testDeleteResource() throws Exception {
        TUma.deleteResource(getApiTagetURI(url), pat, umaRegisterResourcePath, resourceStatus.getId());
    }
}
