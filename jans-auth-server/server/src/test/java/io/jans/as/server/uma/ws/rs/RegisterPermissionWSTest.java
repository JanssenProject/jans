/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.uma.TUma;
import io.jans.as.server.util.ServerUtil;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterPermissionWSTest extends BaseTest {

    @ArquillianResource
    private URI url;

    private static Token pat;
    private static UmaResourceResponse resource;
    private static String umaRegisterResourcePath;
    private static String umaPermissionPath;

    @Test
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
            "umaRedirectUri", "umaRegisterResourcePath", "umaPermissionPath"})
    public void init_(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                      String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath,
                      String p_umaPermissionPath) {
        RegisterPermissionWSTest.umaRegisterResourcePath = umaRegisterResourcePath;
        umaPermissionPath = p_umaPermissionPath;

        pat = TUma.requestPat(getApiTagetURI(url), authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
                umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assertIt(pat);
    }

    @Test(dependsOnMethods = {"init_"})
    public void init() {
        resource = TUma.registerResource(getApiTagetURI(url), pat, umaRegisterResourcePath, UmaTestUtil.createResource());
        UmaTestUtil.assertIt(resource);
    }

    @Test(dependsOnMethods = {"init"})
    public void testRegisterPermission() throws Exception {
        final UmaPermission r = new UmaPermission();
        r.setResourceId(resource.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        final PermissionTicket ticket = TUma.registerPermission(getApiTagetURI(url), pat, r, umaPermissionPath);
        UmaTestUtil.assertIt(ticket);
    }

    @Test(dependsOnMethods = {"testRegisterPermission"})
    public void testRegisterPermissionWithInvalidResource() {
        final String path = umaPermissionPath;
        try {
            Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + path).request();
            request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
            request.header("Authorization", "Bearer " + pat.getAccessToken());

            String json = null;
            try {
                final UmaPermission r = new UmaPermission();
                r.setResourceId(resource.getId() + "x");

                json = ServerUtil.createJsonMapper().writeValueAsString(r);
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }

            Response response = request.post(Entity.json(json));
            String entity = response.readEntity(String.class);

            BaseTest.showResponse("UMA : RegisterPermissionWSTest.testRegisterPermissionWithInvalidResource() : ",
                    response, entity);

            assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Unexpected response code.");
            try {
                final PermissionTicket t = ServerUtil.createJsonMapper().readValue(entity, PermissionTicket.class);
                Assert.assertNull(t);
            } catch (Exception e) {
                // it's ok if it fails here, we expect ticket as null.
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    // use normal test instead of @AfterClass because it will not work with
    // ResourceRequestEnvironment seam class which is used
    // behind TUma wrapper.
    @Test(dependsOnMethods = {"testRegisterPermissionWithInvalidResource"})
    public void cleanUp() {
        if (resource != null) {
            TUma.deleteResource(getApiTagetURI(url), pat, umaRegisterResourcePath, resource.getId());
        }
    }
}
