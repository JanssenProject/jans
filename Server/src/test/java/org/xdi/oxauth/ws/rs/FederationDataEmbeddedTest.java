/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.FederationDataRequest;
import org.xdi.oxauth.client.FederationDataResponse;
import org.xdi.oxauth.model.error.IErrorType;
import org.xdi.oxauth.model.federation.FederationErrorResponseType;
import org.xdi.oxauth.model.federation.FederationRequest;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/10/2012
 */

public class FederationDataEmbeddedTest extends BaseTest {

    public static void assertErrorResponse(EnhancedMockHttpServletResponse p_response, IErrorType p_errorType) {
        assertEquals(p_response.getStatus(), 400, "Unexpected response code. Entity: " + p_response.getContentAsString());
        assertNotNull(p_response.getContentAsString(), "The entity is null");

        FederationDataResponse r = new FederationDataResponse();
        r.injectErrorIfExistSilently(p_response.getContentAsString());

        assertEquals(r.getErrorType(), p_errorType);
        assertTrue(StringUtils.isNotBlank(r.getErrorDescription()));
    }

    @Parameters({"federationPath", "federationMetadataId", "federationRpDisplayName", "federationRpRedirectUri"})
    @Test
    public void requestRpJoin(final String federationPath, final String federationMetadataId,
                              final String federationRpDisplayName, final String federationRpRedirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.RP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationRpDisplayName);
                r.setRedirectUri(federationRpRedirectUri);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestRpJoin", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationRpDisplayName", "federationRpRedirectUris"})
    @Test
    public void requestRpJoinWithMultipleUris(final String federationPath, final String federationMetadataId,
                                              final String federationRpDisplayName, final String federationRpRedirectUris) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.RP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationRpDisplayName);
                r.setRedirectUri(federationRpRedirectUris);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestRpJoinWithMultipleUris", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationOpDisplayName", "federationOpId", "federationOpDomain"})
    @Test
    public void requestOpJoin(final String federationPath, final String federationMetadataId, final String federationOpDisplayName,
                              final String federationOpId, final String federationOpDomain) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.OP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationOpDisplayName);
                r.setOpId(federationOpId);
                r.setDomain(federationOpDomain);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestOpJoin", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
            }
        }.run();
    }

    @Parameters({"federationPath", "federationRpDisplayName", "federationRpRedirectUri"})
    @Test
    public void requestRpJoinFailWithInvalidFederationMetadataId(final String federationPath, final String federationRpDisplayName,
                                                                 final String federationRpRedirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.RP);
                r.setFederationId("dsf");
                r.setDisplayName(federationRpDisplayName);
                r.setRedirectUri(federationRpRedirectUri);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestRpJoinFailWithInvalidFederationMetadataId", response);
                assertErrorResponse(response, FederationErrorResponseType.INVALID_FEDERATION_ID);
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationRpRedirectUri"})
    @Test
    public void requestRpJoinFailWithInvalidDisplayName(final String federationPath, final String federationMetadataId,
                                                        final String federationRpRedirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.RP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName("");
                r.setRedirectUri(federationRpRedirectUri);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestRpJoinFailWithInvalidDisplayName", response);
                assertErrorResponse(response, FederationErrorResponseType.INVALID_DISPLAY_NAME);
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationRpDisplayName"})
    @Test
    public void requestRpJoinFailWithInvalidRedirectUri(final String federationPath, final String federationMetadataId,
                                                        final String federationRpDisplayName) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.RP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationRpDisplayName);
                r.setRedirectUri("");

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestRpJoinFailWithInvalidRedirectUri", response);
                assertErrorResponse(response, FederationErrorResponseType.INVALID_REDIRECT_URI);
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationOpDisplayName", "federationOpDomain"})
    @Test
    public void requestOpJoinFailWithInvalidOpId(final String federationPath, final String federationMetadataId,
                                                 final String federationOpDisplayName, final String federationOpDomain) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.OP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationOpDisplayName);
                r.setOpId("");
                r.setDomain(federationOpDomain);

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestOpJoinFailWithInvalidOpId", response);
                assertErrorResponse(response, FederationErrorResponseType.INVALID_OP_ID);
            }
        }.run();
    }

    @Parameters({"federationPath", "federationMetadataId", "federationOpDisplayName", "federationOpId"})
    @Test
    public void requestOpJoinFailWithInvalidDomain(final String federationPath, final String federationMetadataId,
                                                   final String federationOpDisplayName, final String federationOpId) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, federationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                FederationDataRequest r = new FederationDataRequest(FederationRequest.Type.OP);
                r.setFederationId(federationMetadataId);
                r.setDisplayName(federationOpDisplayName);
                r.setOpId(federationOpId);
                r.setDomain("");

                request.addParameters(r.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestOpJoinFailWithInvalidDomain", response);
                assertErrorResponse(response, FederationErrorResponseType.INVALID_DOMAIN);
            }
        }.run();
    }
}
