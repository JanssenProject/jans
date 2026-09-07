/*
 Copyright (c) 2025, Gluu
 Author: Yuriy Z
 */
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.IdentityAssertionType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IdentityAssertion implements IdentityAssertionType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * Called after ID-JAG JWT claims are populated but before the token is signed.
     * Cast idJagAsJwt to Jwt to read or modify claims.
     *
     * @param idJagAsJwt io.jans.as.model.jwt.Jwt — the unsigned ID-JAG JWT
     * @param context    io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to keep changes, false to discard them
     */
    @Override
    public boolean modifyIdJagPayload(Object idJagAsJwt, Object context) {
        Jwt idJag = (Jwt) idJagAsJwt;
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        // Example: add a custom claim to the ID-JAG payload
        idJag.getClaims().setClaim("custom_idp_claim", "value_from_script");

        scriptLogger.info("modifyIdJagPayload executed for client: {}",
                scriptContext.getExecutionContext().getClient().getClientId());
        return true; // return false to discard the changes made above
    }

    /**
     * Called after the token exchange response JSON is built but before it is returned to the client.
     *
     * @param responseAsJsonObject org.json.JSONObject — the token exchange response
     * @param context              io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to keep changes, false to discard them
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        JSONObject response = (JSONObject) responseAsJsonObject;

        // Example: add an extra field to the token exchange response
        response.put("x_custom_field", "value_from_script");

        scriptLogger.info("modifyResponse executed, response keys: {}", response.keySet());
        return true; // return false to discard the changes made above
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
