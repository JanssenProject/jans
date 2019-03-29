/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws.rs.uma;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaPermissionService;
import org.gluu.oxauth.model.uma.*;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.uma.UmaTestUtil;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNull;

/**
 * Test cases for the registering UMA permissions flow (HTTP)
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */
public class UmaRegisterPermissionFlowHttpTest extends BaseTest {

    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected String ticket;
    protected UmaPermissionService permissionService;

    public UmaRegisterPermissionFlowHttpTest() {
    }

    public UmaRegisterPermissionFlowHttpTest(UmaMetadata metadataConfiguration) {
        this.metadata = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        if (this.metadata == null) {
            this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientExecutor(true)).getMetadata();
            UmaTestUtil.assert_(this.metadata);
        }

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.setAuthorizationEndpoint(authorizationEndpoint);
        this.registerResourceTest.setTokenEndpoint(tokenEndpoint);
        this.registerResourceTest.init(umaMetaDataUrl, umaPatClientId, umaPatClientSecret);

        this.registerResourceTest.addResource();
    }

    @AfterClass
    public void clean() throws Exception {
        this.registerResourceTest.deleteResource();
    }

    public UmaPermissionService getPermissionService() throws Exception {
        if (permissionService == null) {
            permissionService = UmaClientFactory.instance().createPermissionService(this.metadata, clientExecutor(true));
        }
        return permissionService;
    }

    /**
     * Test for registering permissions for resource
     */
    @Test
    public void testRegisterPermission() throws Exception {
        showTitle("testRegisterPermission");
        registerResourcePermission(this.registerResourceTest.resourceId, Arrays.asList("http://photoz.example.com/dev/scopes/view"));
    }

    public String registerResourcePermission(String resourceId, List<String> scopes) throws Exception {

        UmaPermission permission = new UmaPermission();
        permission.setResourceId(resourceId);
        permission.setScopes(scopes);

        PermissionTicket ticket = getPermissionService().registerPermission(
                "Bearer " + this.registerResourceTest.pat.getAccessToken(), UmaPermissionList.instance(permission));
        UmaTestUtil.assert_(ticket);
        this.ticket = ticket.getTicket();
        return ticket.getTicket();
    }

    /**
     * Test for registering permissions for resource
     */
    @Test
    public void testRegisterPermissionForInvalidResource() throws Exception {
        showTitle("testRegisterPermissionForInvalidResource");

        UmaPermission permission = new UmaPermission();
        permission.setResourceId(this.registerResourceTest.resourceId + "1");
        permission.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

        PermissionTicket ticket = null;
        try {
            ticket = getPermissionService().registerPermission(
                    "Bearer " + this.registerResourceTest.pat.getAccessToken(), UmaPermissionList.instance(permission));
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertTrue(ex.getResponse().getStatus() != Response.Status.CREATED.getStatusCode() &&
                    ex.getResponse().getStatus() != Response.Status.OK.getStatusCode()
                    , "Unexpected response status");
        }

        assertNull(ticket, "Resource permission is not null");
    }
}