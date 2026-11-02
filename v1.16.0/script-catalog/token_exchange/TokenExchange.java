/*
 Copyright (c) 2025, Gluu
 Author: Yuriy Z
 */
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.ScriptTokenExchangeControl;
import io.jans.model.custom.script.type.token.TokenExchangeType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TokenExchange implements TokenExchangeType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public ScriptTokenExchangeControl validate(Object context) {

        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        String audience = scriptContext.getRequestParameter("audience");
        String subjectToken = scriptContext.getRequestParameter("subject_token");
        String subjectTokenType = scriptContext.getRequestParameter("subject_token_type");
        String deviceSecret = scriptContext.getRequestParameter("actor_token");
        String actorTokenType = scriptContext.getRequestParameter("actor_token_type");

        // perform all validations here
        boolean validationFailed = false;
        if (validationFailed) {
            return ScriptTokenExchangeControl.fail();
        }

        // user identified by request information or otherwise null
        User user = new User();

        return ScriptTokenExchangeControl
                .ok()
                .setSkipBuiltinValidation(true) // this skip build-in validations of all parameters that come
                .setUser(user);
    }

    /**
     * @param responseAsJsonObject - response represented by org.json.JSONObject
     * @param context              - script context represented by io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if changes must be applied to final response or false if whatever made in this method has to be cancelled
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_script", "value_from_script");
        return true; // return false if you wish to cancel whatever modification was made before
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TokenExchange Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TokenExchange Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed TokenExchange Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
