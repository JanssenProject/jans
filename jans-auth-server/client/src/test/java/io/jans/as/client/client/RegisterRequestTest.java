/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.register.RegisterRequestParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RegisterRequestTest {

    @Test
    public void fromJson_forEvidence_ShouldReturnCorrectValue() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("evidence", "{\"data\":\"somevalue\"}");

        final RegisterRequest registerRequest = RegisterRequest.fromJson(jsonObject.toString());

        assertEquals(registerRequest.getEvidence(), "{\"data\":\"somevalue\"}");
    }

    @Test
    public void getParametersForAdditionalAudienceShouldReturnCorrectValue() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        assertEquals(new JSONArray(Lists.newArrayList("aud1", "aud2")).toString(), request.getParameters().get("additional_audience"));
    }

    @Test
    public void fromJsonForAdditionalAudienceShouldReturnCorrectValue() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("additional_audience", new JSONArray(Lists.newArrayList("aud1", "aud2")));

        final RegisterRequest registerRequest = RegisterRequest.fromJson(jsonObject.toString());

        assertEquals(Lists.newArrayList("aud1", "aud2"), registerRequest.getAdditionalAudience());
    }

    @Test
    public void getJSONParametersForAdditionalAudienceShouldReturnCorrectValue() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        assertEquals(Lists.newArrayList("aud1", "aud2"), ((JSONArray) request.getJSONParameters().get("additional_audience")).toList());
    }

    @Test
    public void getJSONParameters_forBackchannelLogoutUri_shouldReturnCorrectValue() {
        final String value = "https://back.com/b1";

        RegisterRequest request = new RegisterRequest();
        request.setBackchannelLogoutUri(value);

        assertEquals(value, request.getJSONParameters().optString(RegisterRequestParam.BACKCHANNEL_LOGOUT_URI.getName()));
    }

    @Test
    public void fromJson_forAdditionalTokenEndpointAuthMethod_shouldReturnCorrectValue() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("additional_token_endpoint_auth_methods", new JSONArray(Lists.newArrayList("client_secret_basic")));

        final RegisterRequest registerRequest = RegisterRequest.fromJson(jsonObject.toString());

        assertEquals(Lists.newArrayList(AuthenticationMethod.CLIENT_SECRET_BASIC), registerRequest.getAdditionalTokenEndpointAuthMethods());
    }

    @Test
    public void getJSONParameters_forAdditionalTokenEndpointAuthMethod_shouldReturnCorrectValue() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalTokenEndpointAuthMethods(Lists.newArrayList(AuthenticationMethod.CLIENT_SECRET_BASIC));

        assertEquals(Lists.newArrayList("client_secret_basic"), request.getJSONParameters().getJSONArray(RegisterRequestParam.ADDITIONAL_TOKEN_ENDPOINT_AUTH_METHODS.getName()).toList());
    }
}
