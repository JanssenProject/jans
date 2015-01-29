/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.FederationMetadataClient;
import org.xdi.oxauth.client.FederationMetadataRequest;
import org.xdi.oxauth.client.FederationMetadataResponse;
import org.xdi.oxauth.model.federation.FederationOP;
import org.xdi.oxauth.model.federation.FederationRP;

import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/09/2012
 */

public class FederationMetadataWebServiceHttpTest extends BaseTest {

    @Test
    public void requestExistingFederationMetadataIdList() throws Exception {
        showTitle("requestExistingFederationMetadataIdList");

        FederationMetadataClient client = new FederationMetadataClient(federationMetadataEndpoint);
        FederationMetadataResponse response = client.execGetMetadataIds();

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertTrue(response.getExistingMetadataIdList() != null && !response.getExistingMetadataIdList().isEmpty(),
                "MetadataId list is empty. It's expected to have not empty list");
    }

    @Parameters({"federationMetadataId"})
    @Test
    public void requestFederationMetadataById(final String federationMetadataId) throws Exception {
        showTitle("requestFederationMetadataId");

        FederationMetadataRequest request = new FederationMetadataRequest();
        request.setSigned(false);
        request.setFederationId(federationMetadataId);

        FederationMetadataClient client = new FederationMetadataClient(federationMetadataEndpoint);
        FederationMetadataResponse response = client.exec(request);

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getMetadata(), "The metadata is null");
        assertNotNull(response.getMetadata().getId(), "The metadata id is null");
        assertNotNull(response.getMetadata().getDisplayName(), "The metadata displayName is null");
        assertNotNull(response.getMetadata().getIntervalCheck(), "The metadata intervalCheck is not set");

        final List<FederationRP> rpList = response.getMetadata().getRpList();
        final List<FederationOP> opList = response.getMetadata().getOpList();

        assertTrue(rpList != null && !rpList.isEmpty(), "The metadata rp list is not set");
        assertTrue(opList != null && !opList.isEmpty(), "The metadata op list is not set");

        assertNotNull(rpList.get(0).getDisplayName(), "The metadata rp's display_name attribute is not set");
        assertNotNull(rpList.get(0).getRedirectUri(), "The metadata rp's redirect_uri attribute is not set");
        assertNotNull(opList.get(0).getDisplayName(), "The metadata op's display_name attribute is not set");
        assertNotNull(opList.get(0).getOpId(), "The metadata op's op_id attribute is not set");
        assertNotNull(opList.get(0).getDomain(), "The metadata op's domain attribute is not set");
    }

    @Parameters({"federationMetadataId"})
    @Test
    public void requestFederationMetadataByIdNotSigned(final String federationMetadataId) throws Exception {
        showTitle("requestFederationMetadataId");

        FederationMetadataClient client = new FederationMetadataClient(federationMetadataEndpoint);

        FederationMetadataRequest request = new FederationMetadataRequest();
        request.setFederationId(federationMetadataId);
        request.setSigned(false);
        FederationMetadataResponse response = client.exec(request);

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getMetadata(), "The metadata is null");
        assertNotNull(response.getMetadata().getId(), "The metadata id is null");
        assertNotNull(response.getMetadata().getDisplayName(), "The metadata displayName is null");
        assertNotNull(response.getMetadata().getIntervalCheck(), "The metadata intervalCheck is not set");

        final List<FederationRP> rpList = response.getMetadata().getRpList();
        final List<FederationOP> opList = response.getMetadata().getOpList();

        assertTrue(rpList != null && !rpList.isEmpty(), "The metadata rp list is not set");
        assertTrue(opList != null && !opList.isEmpty(), "The metadata op list is not set");

        assertNotNull(rpList.get(0).getDisplayName(), "The metadata rp's display_name attribute is not set");
        assertNotNull(rpList.get(0).getRedirectUri(), "The metadata rp's redirect_uri attribute is not set");
        assertNotNull(opList.get(0).getDisplayName(), "The metadata op's display_name attribute is not set");
        assertNotNull(opList.get(0).getOpId(), "The metadata op's op_id attribute is not set");
        assertNotNull(opList.get(0).getDomain(), "The metadata op's domain attribute is not set");
    }

    @Test
    public void requestFederationMetadataByIdFail() throws Exception {
        showTitle("requestFederationMetadataIdFail");

        FederationMetadataClient client = new FederationMetadataClient(federationMetadataEndpoint);
        FederationMetadataResponse response = client.execGetMetadataById("notExistingId");

        showClient(client);
        assertEquals(response.getStatus(), 400, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }
}