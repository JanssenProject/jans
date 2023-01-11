/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.verifier.CommonVerifiers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * serves request for /attestation endpoint exposed by FIDO2 sever
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
@Path("/attestation")
public class AttestationController {

    @Inject
    private AttestationService attestationService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response register(String content) {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params;
        try {
        	params = dataMapperService.readTree(content);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse options attestation request", ex);
        }

        JsonNode result = attestationService.options(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(String content) {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params;
        try {
        	params = dataMapperService.readTree(content);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse finish attestation request", ex) ;
        }

        JsonNode result = attestationService.verify(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    /* Example for one_step:
     *  - request:
     *             username: null
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: a5183b05-dbe0-4173-a794-2334bb864708
     *             enrollment_code: null
     *  - response:
     *             {"authenticateRequests":[],"registerRequests":[{"challenge":"GU4usvpYfvQ_RCMSqm819gTZMa0qCeLr1Xg2KbvW2To"
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm","version":"U2F_V2"}]}
     *            
     * Example for two_step:
     *  - request:
     *             username: test1
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 46c30db8-0339-4459-8ee7-e4960ad75986
     *             enrollment_code: null
     *  - response:
     *             {"authenticateRequests":[],"registerRequests":[{"challenge":"raPfmqZOHlHF4gXbprd29uwX-bs3Ff5v03quxBD4FkM",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm","version":"U2F_V2"}]}
     */
    @GET
    @Produces({ "application/json" })
    @Path("/registration")
    public Response startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId, @QueryParam("enrollment_code") String enrollmentCode) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isUseSuperGluu()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);

        JsonNode result = attestationService.options(params);

        // Build start registration response  
        JsonNode superGluuResult = result;

        ResponseBuilder builder = Response.ok().entity(superGluuResult.toString());
        return builder.build();
    }

    /* Example for one_step:
     *  - request:
     *             username: null
     *             tokenResponse: {"registrationData":"BQQTkZFzsbTmuUoS_DS_jqpWRbZHp_J0YV8q4Xb4XTPYbIuvu-TRNubp8U-CKZuB
     *             5tDT-l6R3sQvNc6wXjGCmL-OQK-ACAQk_whIvElk4Sfk-YLMY32TKs6jHagng7iv8U78_5K-RTTbNo-k29nb6F4HLpbYcU81xefh
     *             SNSlrAP3USwwggImMIIBzKADAgECAoGBAPMsD5b5G58AphKuKWl4Yz27sbE_rXFy7nPRqtJ_r4E5DSZbFvfyuos-Db0095ubB0Jo
     *             yM8ccmSO_eZQ6IekOLPKCR7yC5kes-f7MaxyaphmmD4dEvmuKjF-fRsQP5tQG7zerToto8eIz0XjPaupiZxQXtSHGHHTuPhri2nf
     *             oZlrMAoGCCqGSM49BAMCMFwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQH
     *             EwZBdXN0aW4xCzAJBgNVBAgTAlRYMQswCQYDVQQGEwJVUzAeFw0xNjAzMDExODU5NDZaFw0xOTAzMDExODU5NDZaMFwxIDAeBgNV
     *             BAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVBAgTAlRYMQsw
     *             CQYDVQQGEwJVUzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABICUKnzCE5PJ7tihiKkYu6E5Uy_sZ-RSqs_MnUJt0tB8G8GSg9nK
     *             o6P2424iV9lXX9Pil8qw4ofZ-fAXXepbp4MwCgYIKoZIzj0EAwIDSAAwRQIgUWwawAB2udURWQziDXVjSOi_QcuXiRxylqj5thFw
     *             FhYCIQCGY-CTZFi7JdkhZ05nDpbSYJBTOo1Etckh7k0qcvnO0TBFAiEA1v1jKTwGn5LRRGSab1kNdgEqD6qL08bougoJUNY1A5MC
     *             IGvtBFSNzhGvhQmdYYj5-XOd5P4ucVk6TmkV1Xu73Dvj","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZmluaXNoRW5y
     *             b2xsbWVudCIsImNoYWxsZW5nZSI6IkdVNHVzdnBZZnZRX1JDTVNxbTgxOWdUWk1hMHFDZUxyMVhnMktidlcyVG8iLCJvcmlnaW4i
     *             OiJodHRwczpcL1wveXVyZW0tZW1lcmdpbmctcGlnLmdsdXUuaW5mbyJ9","deviceData":"eyJuYW1lIjoiU00tRzk5MUIiLCJv
     *             c19uYW1lIjoidGlyYW1pc3UiLCJvc192ZXJzaW9uIjoiMTMiLCJwbGF0Zm9ybSI6ImFuZHJvaWQiLCJwdXNoX3Rva2VuIjoicHVz
     *             aF90b2tlbiIsInR5cGUiOiJub3JtYWwiLCJ1dWlkIjoidXVpZCJ9"}
     *  - response:
     *             {"status":"success","challenge":"GU4usvpYfvQ_RCMSqm819gTZMa0qCeLr1Xg2KbvW2To"}
     *            
    * Example for two_step:
     *  - request:
     *             username: test1
     *             tokenResponse: {"registrationData":"BQToXkGAjgXxC4g1NiA-IuRAu40NFBlXXNSu4TEZqGK5TBqwU07ANn4LJ9Hp3aV5
     *             PIvCDVsQ2tZJf1xD6LosZNDuQGCb1g_Z-NHiLqyJybzylKMaSs65XlDFoLvcN7_zVAbuwYhgHJYJB_2YPPP0-cukYe_Ipk8U4dHY
     *             1hWBuI8ToscwggImMIIBzKADAgECAoGBAPMsD5b5G58AphKuKWl4Yz27sbE_rXFy7nPRqtJ_r4E5DSZbFvfyuos-Db0095ubB0Jo
     *             yM8ccmSO_eZQ6IekOLPKCR7yC5kes-f7MaxyaphmmD4dEvmuKjF-fRsQP5tQG7zerToto8eIz0XjPaupiZxQXtSHGHHTuPhri2nf
     *             oZlrMAoGCCqGSM49BAMCMFwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQH
     *             EwZBdXN0aW4xCzAJBgNVBAgTAlRYMQswCQYDVQQGEwJVUzAeFw0xNjAzMDExODU5NDZaFw0xOTAzMDExODU5NDZaMFwxIDAeBgNV
     *             BAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVBAgTAlRYMQsw
     *             CQYDVQQGEwJVUzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABICUKnzCE5PJ7tihiKkYu6E5Uy_sZ-RSqs_MnUJt0tB8G8GSg9nK
     *             o6P2424iV9lXX9Pil8qw4ofZ-fAXXepbp4MwCgYIKoZIzj0EAwIDSAAwRQIgUWwawAB2udURWQziDXVjSOi_QcuXiRxylqj5thFw
     *             FhYCIQCGY-CTZFi7JdkhZ05nDpbSYJBTOo1Etckh7k0qcvnO0TBFAiArOYmHd22USw7flCmGXLOXVOrhDi-pkX7Qx_c8oz5hJQIh
     *             AOslo3LfymoFWT6mxUZjBlxKgxioozd0KmzUwobRcKdW","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZmluaXNoRW5y
     *             b2xsbWVudCIsImNoYWxsZW5nZSI6InJhUGZtcVpPSGxIRjRnWGJwcmQyOXV3WC1iczNGZjV2MDNxdXhCRDRGa00iLCJvcmlnaW4i
     *             OiJodHRwczpcL1wveXVyZW0tZW1lcmdpbmctcGlnLmdsdXUuaW5mbyJ9","deviceData":"eyJuYW1lIjoiU00tRzk5MUIiLCJv
     *             c19uYW1lIjoidGlyYW1pc3UiLCJvc192ZXJzaW9uIjoiMTMiLCJwbGF0Zm9ybSI6ImFuZHJvaWQiLCJwdXNoX3Rva2VuIjoicHVz
     *             aF90b2tlbiIsInR5cGUiOiJub3JtYWwiLCJ1dWlkIjoidXVpZCJ9"}
     *  - response:
     *             {"status":"success","challenge":"raPfmqZOHlHF4gXbprd29uwX-bs3Ff5v03quxBD4FkM"}
     *            
     */
    @GET
    @Produces({ "application/json" })
    @Path("/registration")
    public Response finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isUseSuperGluu()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);

        JsonNode result = attestationService.verify(params);

        // Build finish registration response
        JsonNode superGluuResult = result;

        ResponseBuilder builder = Response.ok().entity(superGluuResult.toString());
        return builder.build();
    }

}
