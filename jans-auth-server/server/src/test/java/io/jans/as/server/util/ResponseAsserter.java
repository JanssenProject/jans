package io.jans.as.server.util;

import io.jans.as.model.register.RegisterResponseParam;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.register.RegisterResponseParam.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author yuriyz
 */
public class ResponseAsserter {

    private final int status;
    private final String entity;

    private JSONObjectAsserter json;

    public ResponseAsserter(int status, String entity) {
        this.status = status;
        this.entity = entity;
    }

    public static ResponseAsserter of(int status, String entity) {
        return new ResponseAsserter(status, entity);
    }

    public ResponseAsserter assertStatus(int expectedStatusCode) {
        assertEquals(status, expectedStatusCode, "Unexpected status code: " + status);
        return this;
    }

    public ResponseAsserter assertStatusOk() {
        assertStatus(200);
        return this;
    }

    public JSONObjectAsserter assertJsonObject() {
        try {
            return JSONObjectAsserter.of(new JSONObject(entity));
        } catch (JSONException e) {
            fail(e.getMessage() + "\nResponse was: " + entity, e);
            throw new AssertionError(e);
        }
    }

    public ResponseAsserter assertRegisterResponse() {
        assertStatus(201);
        json = assertJsonObject();
        json.hasKeys(RegisterResponseParam.CLIENT_ID.toString(),
                CLIENT_SECRET.toString(),
                REGISTRATION_ACCESS_TOKEN.toString(),
                REGISTRATION_CLIENT_URI.toString(),
                CLIENT_ID_ISSUED_AT.toString(),
                CLIENT_SECRET_EXPIRES_AT.toString()
        );
        return this;
    }

    public JSONObjectAsserter getJson() {
        return json;
    }

    public int getStatus() {
        return status;
    }

    public String getEntity() {
        return entity;
    }
}
