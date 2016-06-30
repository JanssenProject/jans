/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.FederationMetadataClient;
import org.xdi.oxauth.client.FederationMetadataRequest;
import org.xdi.oxauth.client.FederationMetadataResponse;
import org.xdi.oxauth.model.federation.FederationMetadata;
import org.xdi.oxauth.model.federation.FederationOP;
import org.xdi.oxauth.model.federation.FederationRP;

import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/09/2012
 */

public class FederationMetadataEmbeddedTest extends BaseTest {

    @Parameters({"federationMetadataPath"})
    @Test
    public void requestExistingFederationMetadataIdList(final String federationMetadataPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, federationMetadataPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                final FederationMetadataRequest r = new FederationMetadataRequest();
                request.setQueryString(r.getQueryString());

                output("Request url:" + request.getRequestURI());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestExistingFederationMetadataIdList", response);

                final String contentAsString = response.getContentAsString();

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertNotNull(contentAsString, "Unexpected result: " + contentAsString);
                try {
                    final List<String> list = FederationMetadataClient.convertMetadataIdList(contentAsString);
                    assertTrue(list != null && !list.isEmpty(), "MetadataId list is empty. It's expected to have not empty list");
                } catch (JSONException e) {
                    fail(e.getMessage() + "\nResponse was: " + contentAsString, e);
                } catch (Exception e) {
                    fails(e);
                }
            }
        }.run();
    }

    @Parameters({"federationMetadataPath", "federationMetadataId"})
    @Test
    public void requestFederationMetadataByIdSigned(final String federationMetadataPath, final String federationMetadataId) throws Exception {
        baseTest(federationMetadataPath, federationMetadataId, true);
    }

    @Parameters({"federationMetadataPath", "federationMetadataId"})
    @Test
    public void requestFederationMetadataByIdNotSigned(final String federationMetadataPath, final String federationMetadataId) throws Exception {
        baseTest(federationMetadataPath, federationMetadataId, false);
    }

    @Parameters({"federationMetadataPath"})
    @Test
    public void requestFederationMetadataByIdFail(final String federationMetadataPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, federationMetadataPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                final FederationMetadataRequest r = new FederationMetadataRequest("notExistingId");
                request.setQueryString(r.getQueryString());

                output("Request url:" + request.getRequestURI());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestFederationMetadataByIdFail", response);

                final String contentAsString = response.getContentAsString();
                final FederationMetadataResponse federationResponse = new FederationMetadataResponse();

                federationResponse.injectErrorIfExistSilently(contentAsString);
                assertEquals(response.getStatus(), 400, "Unexpected response code. Entity: " + contentAsString);
                assertNotNull(contentAsString, "The entity is null");
                assertNotNull(federationResponse.getErrorType(), "The error type is null");
                assertNotNull(federationResponse.getErrorDescription(), "The error description is null");
            }
        }.run();
    }

    private void baseTest(final String federationMetadataPath, final String federationMetadataId, final boolean p_signed) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, federationMetadataPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                final FederationMetadataRequest r = new FederationMetadataRequest(federationMetadataId);
                r.setSigned(p_signed);
                request.setQueryString(r.getQueryString());

                output("Request url:" + request.getRequestURI());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestFederationMetadataByIdSigned", response);

                final String contentAsString = response.getContentAsString();

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertNotNull(contentAsString, "Unexpected result: " + contentAsString);
                try {
                    FederationMetadataResponse r = new FederationMetadataResponse();
                    FederationMetadataClient.fillResponse(r, contentAsString, p_signed);

                    final FederationMetadata metadata = r.getMetadata();
                    assertNotNull(metadata, "The metadata is null");
                    assertNotNull(metadata.getId(), "The metadata id is null");
                    assertNotNull(metadata.getDisplayName(), "The metadata displayName is null");
                    assertNotNull(metadata.getIntervalCheck(), "The metadata intervalCheck is not set");

                    final List<FederationRP> rpList = metadata.getRpList();
                    final List<FederationOP> opList = metadata.getOpList();

                    assertTrue(rpList != null && !rpList.isEmpty(), "The metadata rp list is not set");
                    assertTrue(opList != null && !opList.isEmpty(), "The metadata op list is not set");

                    assertNotNull(rpList.get(0).getDisplayName(), "The metadata rp's display_name attribute is not set");
                    assertNotNull(rpList.get(0).getRedirectUri(), "The metadata rp's redirect_uri attribute is not set");
                    assertNotNull(opList.get(0).getDisplayName(), "The metadata op's display_name attribute is not set");
                    assertNotNull(opList.get(0).getOpId(), "The metadata op's op_id attribute is not set");
                    assertNotNull(opList.get(0).getDomain(), "The metadata op's domain attribute is not set");
                } catch (Exception e) {
                    fails(e);
                }
            }
        }.run();
    }
}
