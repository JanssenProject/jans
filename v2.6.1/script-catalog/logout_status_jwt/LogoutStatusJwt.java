import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.LogoutStatusJwtType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class LogoutStatusJwt implements LogoutStatusJwtType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     *
     * @param jsonWebResponse refers to io.jans.as.model.token.JsonWebResponse
     * @param context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if logout_status_jwt should be created or false to forbid logout_status_jwt creation.
     */
    @Override
    public boolean modifyPayload(Object jsonWebResponse, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        JsonWebResponse jwr = (JsonWebResponse) jsonWebResponse;
        jwr.getClaims().setClaim("custom_claim", "custom_value");

        return true;
    }

    /**
     *
     * @param context context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return lifetime of logout_status_jwt in seconds. It must be more then 0 or otherwise it will be ignored by server.
     */
    @Override
    public int getLifetimeInSeconds(Object context) {
        boolean condition = false; // under some condition return 1 day lifetime
        if (condition) {
            return 86400;
        }
        return 0;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized LogoutStatusJwt Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized LogoutStatusJwt Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed LogoutStatusJwt Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}