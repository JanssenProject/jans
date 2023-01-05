/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.register.RegisterRequestParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RegisterRequestTest {

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

        assertEquals(Lists.newArrayList("aud1", "aud2"), request.getJSONParameters().get("additional_audience"));
    }

    @Test
    public void getJSONParameters_forBackchannelLogoutUri_shouldReturnCorrectValue() {
        final List<String> value = Lists.newArrayList("https://back.com/b1", "https://back.com/b2");

        RegisterRequest request = new RegisterRequest();
        request.setBackchannelLogoutUris(value);

        assertEquals(value, request.getJSONParameters().getJSONArray(RegisterRequestParam.BACKCHANNEL_LOGOUT_URI.getName()).toList());
    }
}
