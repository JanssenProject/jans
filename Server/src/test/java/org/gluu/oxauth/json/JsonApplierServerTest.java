package org.gluu.oxauth.json;

import com.google.common.collect.Lists;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.model.json.JsonApplier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oxauth.persistence.model.ClientAttributes;
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
