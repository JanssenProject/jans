
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.par.ParType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class Par implements ParType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean createPar(Object par, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        io.jans.as.persistence.model.Par parObject = (io.jans.as.persistence.model.Par) par;
        parObject.getAttributes().setScope("openid profile");

        if ("bad".equalsIgnoreCase(scriptContext.getExecutionContext().getClient().getClientId())) {
            scriptContext.setWebApplicationException(
                    new NoLogWebApplicationException(Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Forbidden by custom script.")
                            .build()));
        }

        return true;
    }

    @Override
    public boolean modifyParResponse(Object responseAsJsonObject, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        JSONObject json = (JSONObject) responseAsJsonObject;
        json.accumulate("custom_key", "custom_value");
        return true;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized PAR Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized PAR Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed PAR Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
