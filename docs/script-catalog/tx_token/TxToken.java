
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.TxTokenType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TxToken implements TxTokenType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     *
     * @param context context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return lifetime of tx_token in seconds. It must be more then 0 or otherwise it will be ignored by server.
     */
    @Override
    public int getTxTokenLifetimeInSeconds(Object context) {
        boolean condition = false; // under some condition return 1 day lifetime
        if (condition) {
            return 86400;
        }
        return 0;
    }

    /**
     *
     * @param jsonWebResponse refers to io.jans.as.model.token.JsonWebResponse
     * @param context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if tx_token should be created or false to forbid tx_token creation.
     */
    @Override
    public boolean modifyTokenPayload(Object jsonWebResponse, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        JsonWebResponse jwr = (JsonWebResponse) jsonWebResponse;
        jwr.getClaims().setClaim("custom_claim", "custom_value");

        return true;
    }

    /**
     * @param responseAsJsonObject - response represented by org.json.JSONObject
     * @param context              - script context represented by io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if changes must be applied to final response or false if whatever made in this method has to be cancelled
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        JSONObject json = (JSONObject) responseAsJsonObject;
        json.accumulate("custom_key", "custom_value");
        return true;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TxToken Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TxToken Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed TxToken Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
