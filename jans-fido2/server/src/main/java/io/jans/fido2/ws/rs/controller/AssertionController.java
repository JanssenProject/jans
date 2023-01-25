/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.sg.RawAuthenticationService;
import io.jans.fido2.service.verifier.CommonVerifiers;
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

/**
 * serves request for /assertion endpoint exposed by FIDO2 sever 
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
@Path("/assertion")
public class AssertionController {

    @Inject
    private AssertionService assertionService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;
    
    @Inject
    private RawAuthenticationService rawAuthenticationService;

    @Inject
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response authenticate(String content) {
        if (appConfiguration.getFido2Configuration() == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params;
        try {
        	params = dataMapperService.readTree(content);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse options assertion request", ex);
        }

        JsonNode result = assertionService.options(params);

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
            throw new Fido2RpRuntimeException("Failed to parse finish assertion request", ex);
        }

        JsonNode result = assertionService.verify(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    /* Example for one_step:
     *  - request:
     *             username: null
     *             keyhandle: r4AIBCT_CEi8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 2994e597-3dc9-4d96-ae7e-84cfcf049db6
     *  - response:
     *             {"authenticateRequests":[{"challenge":"EELAH05XTUfPHrvpqVYhXB8pEmOMaRWY9mBurdhicBU",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm",
     *             "keyHandle":"r4AIBCT_CEi8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA","version":"U2F_V2"}]}
     *            
     * Example for two_step:
     *  - request:
     *             username: test1
     *             keyhandle: null
     *             application: https://yurem-emerging-pig.gluu.info/identity/authcode.htm
     *             session_id: 850ff665-02b6-435b-baf8-b018b13043c3
     *  - response:
     *             {"authenticateRequests":[{"challenge":"5QoRtudmej5trcrMRgFBoI5rZ6pzIZiYP3u3bXCvvAE",
     *             "appId":"https://yurem-emerging-pig.gluu.info/identity/authcode.htm",
     *             "keyHandle":"YJvWD9n40eIurInJvPKUoxpKzrleUMWgu9w3v_NUBu7BiGAclgkH_Zg88_T5y6Rh78imTxTh0djWFYG4jxOixw","version":"U2F_V2"}]}
     */
    @GET
    @Produces({ "application/json" })
    @Path("/authentication")
    public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);
        params.put(CommonVerifiers.SUPER_GLUU_APP_ID, appId);

        // TODO: Validate input parameters
        params.put("username", userName);
        params.put("session_id", sessionId);

        ObjectNode result = assertionService.options(params);

        // Build start authentication response  
        ObjectNode superGluuResult = dataMapperService.createObjectNode();
        ArrayNode authenticateRequests = superGluuResult.putArray("authenticateRequests");

        String rpId = result.get("rpId").asText();
        String challenge = result.get("challenge").asText();
        String userVerification = result.get("userVerification").asText();

        if (result.has("allowCredentials")) {
	        	result.get("allowCredentials").forEach((f) -> {
	        		((ObjectNode) f).put("appId", appId);
	        		((ObjectNode) f).put("userVerification", userVerification);
	        		((ObjectNode) f).put("challenge", challenge);
	        		((ObjectNode) f).put("keyHandle", f.get("id").asText());
	        		((ObjectNode) f).remove("id");
	        		((ObjectNode) f).put("version", "U2F_V2");

	        		authenticateRequests.add(f);
	        	});
        }

        ResponseBuilder builder = Response.ok().entity(superGluuResult.toString());
        return builder.build();
    }

    /* Example for one_step:
     *  - request:
     *             username: null
     *             tokenResponse: {"signatureData":"AQAAAAEwRQIhANrCm98JCTz6cqSZ_vwGHdF9uqe3b4z1nCrNIPCObwc-AiAblGdWyky
     *             LeaTJPzLtbWHMoN9MsKUlgmbfSRsINJEVeA","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwiY
     *             2hhbGxlbmdlIjoiRUVMQUgwNVhUVWZQSHJ2cHFWWWhYQjhwRW1PTWFSV1k5bUJ1cmRoaWNCVSIsIm9yaWdpbiI6Imh0dHBzOlwvX
     *             C95dXJlbS1lbWVyZ2luZy1waWcuZ2x1dS5pbmZvXC9pZGVudGl0eVwvYXV0aGNvZGUuaHRtIn0","keyHandle":"r4AIBCT_CEi
     *             8SWThJ-T5gsxjfZMqzqMdqCeDuK_xTvz_kr5FNNs2j6Tb2dvoXgculthxTzXF5-FI1KWsA_dRLA"}
     *  - response:
     *             {"status":"success","challenge":"EELAH05XTUfPHrvpqVYhXB8pEmOMaRWY9mBurdhicBU"}
     *            
    * Example for two_step:
     *  - request:
     *             username: test1
     *             tokenResponse: {"signatureData":"AQAAAAEwRgIhAN4auE9-U2YDhi8ByxIIv3G2hvDeFjEGU_x5SvfcIQyUAiEA4I_xMin
     *             mYAmH5qk5KMaYATFAryIpoVwARGvEFQTWE2Q","clientData":"eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwi
     *             Y2hhbGxlbmdlIjoiNVFvUnR1ZG1lajV0cmNyTVJnRkJvSTVyWjZweklaaVlQM3UzYlhDdnZBRSIsIm9yaWdpbiI6Imh0dHBzOlwv
     *             XC95dXJlbS1lbWVyZ2luZy1waWcuZ2x1dS5pbmZvXC9pZGVudGl0eVwvYXV0aGNvZGUuaHRtIn0","keyHandle":"YJvWD9n40e
     *             IurInJvPKUoxpKzrleUMWgu9w3v_NUBu7BiGAclgkH_Zg88_T5y6Rh78imTxTh0djWFYG4jxOixw"}
     *  - response:
     *             {"status":"success","challenge":"5QoRtudmej5trcrMRgFBoI5rZ6pzIZiYP3u3bXCvvAE"}
     *            
     */
    @POST
    @Produces({ "application/json" })
    @Path("/authentication")
    public Response finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString) {
        if ((appConfiguration.getFido2Configuration() == null) && !appConfiguration.isSuperGluuEnabled()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        AuthenticateResponse authenticateResponse;
        try {
        	authenticateResponse = jsonMapperWithWrapRoot().readValue(authenticateResponseString, AuthenticateResponse.class);
        } catch (IOException ex) {
            throw new Fido2RpRuntimeException("Failed to parse options assertion request", ex);
        }

        if (!authenticateResponse.getClientData().getTyp().equals("navigator.id.getAssertion")) {
            throw new Fido2RuntimeException("Invalid options attestation request type");
        }

        ObjectNode params = dataMapperService.createObjectNode();
        // Add all required parameters from request to allow process U2F request 
        params.put(CommonVerifiers.SUPER_GLUU_REQUEST, true);

        // Manadatory parameter
        params.put("type", "public-key");
        
        params.put("id", authenticateResponse.getKeyHandle());

        params.put("rawId", authenticateResponseString);

        // Convert clientData node to new format
        ObjectNode clientData = dataMapperService.createObjectNode();
        clientData.put("type", "webauthn.get");
        clientData.put("challenge", authenticateResponse.getClientData().getChallenge());
        clientData.put("origin", authenticateResponse.getClientData().getOrigin());

        // Add response node
        ObjectNode response = dataMapperService.createObjectNode();
		params.set("response", response);

		// We have to quote URL to conform bug in Super Gluu
		response.put("clientDataJSON", base64Service.urlEncodeToString(clientData.toString().replaceAll("/", "\\\\/").getBytes(StandardCharsets.UTF_8)));
		
		// Prepare attestationObject
		RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData());

		response.put("signature", base64Service.urlEncodeToString(rawAuthenticateResponse.getSignature()));

		ObjectNode attestationObject = dataMapperService.createObjectNode();
        
        try {
	        byte[] authData = generateAuthData(authenticateResponse.getClientData(), rawAuthenticateResponse);
	        response.put("authenticatorData", authData);
	        response.put("attestationObject", base64Service.urlEncodeToString(dataMapperService.cborWriteAsBytes(attestationObject)));
		} catch (IOException e) {
            throw new Fido2RuntimeException("Failed to prepare attestationObject");
		}

        ObjectNode result = assertionService.verify(params);

        result.put("status", "success");
        result.put("challenge", authenticateResponse.getClientData().getChallenge());

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    private byte[] generateAuthData(ClientData clientData, RawAuthenticateResponse rawAuthenticateResponse) throws IOException {
    	byte[] rpIdHash = hash(clientData.getOrigin());
    	byte[] flags = new byte[] { AuthenticatorDataParser.FLAG_USER_PRESENT };
    	byte[] counter = ByteBuffer.allocate(4).putInt((int) rawAuthenticateResponse.getCounter()).array();
        
		byte[] authData = ByteBuffer
				.allocate(rpIdHash.length + flags.length + counter.length)
				.put(rpIdHash).put(flags).put(counter).array();
		
		return authData;
    }

    public byte[] hash(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] hash(String str) {
        return hash(str.getBytes());
    }

    // TODO: User ServerUtil
    public static ObjectMapper createJsonMapper() {
        final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        final AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        final AnnotationIntrospector pair = AnnotationIntrospector.pair(jackson, jaxb);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        return mapper;
    }

    public static ObjectMapper jsonMapperWithWrapRoot() {
        return createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
    }

}
