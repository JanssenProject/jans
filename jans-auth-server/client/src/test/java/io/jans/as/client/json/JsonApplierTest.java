/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.json;

import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.json.JsonApplier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 * @version April 25, 2022
 */
public class JsonApplierTest {

    @Test
    public void apply_forListAndJSONObjectAsTarget_shouldTransferPropertyToTarget() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        JSONObject target = new JSONObject();

        JsonApplier.getInstance().apply(request, target);

        assertEquals(new JSONArray(Lists.newArrayList("aud1", "aud2")), target.getJSONArray("additional_audience"));
    }

    @Test
    public void apply_forListAndMapAsTarget_shouldTransferPropertyToTarget() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        Map<String, String> target = new HashMap<>();

        JsonApplier.getInstance().apply(request, target);

        assertEquals(new JSONArray(Lists.newArrayList("aud1", "aud2")).toString(), target.get("additional_audience"));
    }

    @Test
    public void apply_forListAndJavaObjectAsTarget_shouldTransferPropertyToTarget() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("additional_audience", new JSONArray(Lists.newArrayList("aud1", "aud2")));

        final RegisterRequest registerRequest = new RegisterRequest();
        JsonApplier.getInstance().apply(jsonObject, registerRequest);

        assertEquals(Lists.newArrayList("aud1", "aud2"), registerRequest.getAdditionalAudience());
    }
}
