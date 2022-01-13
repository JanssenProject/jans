/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.json;

import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.persistence.model.ClientAttributes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JsonApplierServerTest {

    @Test
    public void transfer_fromRegisterRequestToClientAttribute_withListOfString_shouldTransferValueCorrectly() {
        RegisterRequest request = new RegisterRequest();
        request.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        ClientAttributes attributes = new ClientAttributes();

        JsonApplier.getInstance().transfer(request, attributes);

        assertEquals(Lists.newArrayList("aud1", "aud2"), attributes.getAdditionalAudience());
    }

    @Test
    public void apply_fromClientAttributesToJSONObject_withListOfString_shouldTransferValueCorrectly() {
        ClientAttributes attributes = new ClientAttributes();
        attributes.setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        JSONObject jsonObject = new JSONObject();

        JsonApplier.getInstance().apply(attributes, jsonObject);

        assertEquals(new JSONArray(Lists.newArrayList("aud1", "aud2")), jsonObject.getJSONArray("additional_audience"));
    }
}
