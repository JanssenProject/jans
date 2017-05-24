/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.UmaPermissionService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaTestUtil;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test cases for the registering UMA resource set description permissions flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/19/2012
 */
public class UmaRegisterPermissionFlowHttpTest extends BaseTest {

    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected String ticketForFullAccess;

    public UmaRegisterPermissionFlowHttpTest() {
    }

    public UmaRegisterPermissionFlowHttpTest(UmaMetadata metadataConfiguration) {
        this.metadata = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaUserId, final String umaUserSecret,
                     final String umaPatClientId, final String umaPatClientSecret, final String umaRedirectUri) throws Exception {
        if (this.metadata == null) {
            this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl).getMetadata();
            UmaTestUtil.assert_(this.metadata);
        }

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.setAuthorizationEndpoint(authorizationEndpoint);
        this.registerResourceTest.setTokenEndpoint(tokenEndpoint);
        this.registerResourceTest.init(umaMetaDataUrl, umaPatClientId, umaPatClientSecret);

        this.registerResourceTest.testRegisterResource();
    }

    @AfterClass
    public void clean() throws Exception {
        this.registerResourceTest.testDeleteResource();
    }

    /**
     * Test for registering permissions for resource set
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testRegisterPermission(final String umaAmHost) throws Exception {
        showTitle("testRegisterPermission");
        registerResourcePermission(umaAmHost, this.registerResourceTest.resourceId, Arrays.asList("http://photoz.example.com/dev/scopes/view"));
    }

    public String registerResourcePermission(final String umaAmHost, String resourceId, List<String> scopes) throws Exception {
        UmaPermissionService permissionService = UmaClientFactory.instance().
                createPermissionService(this.metadata);

        // Register permissions for resource
        UmaPermission permissionRequest = new UmaPermission();
        permissionRequest.setResourceId(resourceId);
        permissionRequest.setScopes(scopes);

        PermissionTicket t = null;
        try {
            t = permissionService.registerPermission(
                    "Bearer " + this.registerResourceTest.pat.getAccessToken(), umaAmHost, permissionRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(t);
        this.ticketForFullAccess = t.getTicket();
        return t.getTicket();
    }

    /**
     * Test for registering permissions for resource
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testRegisterPermissionForInvalidResource(final String umaAmHost) throws Exception {
        showTitle("testRegisterPermissionForInvalidResource");

        UmaPermissionService permissionService = UmaClientFactory.instance().createPermissionService(this.metadata);

        // Register permissions for resource
        UmaPermission permissionRequest = new UmaPermission();
        permissionRequest.setResourceId(this.registerResourceTest.resourceId + "1");
        permissionRequest.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));


        PermissionTicket permissionTicket = null;
        try {
            permissionTicket = permissionService.registerPermission(
                    "Bearer " + this.registerResourceTest.pat.getAccessToken(), umaAmHost, permissionRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode(), "Unexpected response status");
        }

        assertNull(permissionTicket, "Resource set permission is not null");
    }
}