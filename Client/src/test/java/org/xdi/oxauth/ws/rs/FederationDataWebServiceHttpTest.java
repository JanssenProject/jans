package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.FederationDataClient;
import org.xdi.oxauth.client.FederationDataResponse;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/10/2012
 */
public class FederationDataWebServiceHttpTest extends BaseTest {

    @Parameters({"federationMetadataId", "federationRpDisplayName", "federationRpRedirectUri"})
    @Test
    public void requestRpJoin(String federationMetadataId, String federationRpDisplayName, String federationRpRedirectUri) throws Exception {
        showTitle("requestRpJoin");

        FederationDataClient client = new FederationDataClient(federationEndpoint);
        FederationDataResponse response = client.joinRP(federationMetadataId, federationRpDisplayName, federationRpRedirectUri);

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
    }

    @Parameters({"federationMetadataId", "federationRpDisplayName", "federationRpRedirectUris"})
    @Test
    public void requestRpJoinWithMultipleUris(String federationMetadataId, String federationRpDisplayName, String federationRpRedirectUris) throws Exception {
        showTitle("requestRpJoinWithMultipleUris");

        FederationDataClient client = new FederationDataClient(federationEndpoint);
        FederationDataResponse response = client.joinRP(federationMetadataId, federationRpDisplayName, federationRpRedirectUris);

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
    }
}