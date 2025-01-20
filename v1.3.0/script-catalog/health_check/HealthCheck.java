package io.jans.as.server._scripts;

import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.health.HealthCheckType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class HealthCheck implements HealthCheckType {

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public String healthCheck(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        HttpServletRequest httpRequest = scriptContext.getExecutionContext().getHttpRequest();

        String appStatus = "running";
        String dbStatus = "online";
        return String.format("{\"status\": \"%s\", \"db_status\":\"%s\"}", appStatus, dbStatus);
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized HealthCheck Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized HealthCheck Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed HealthCheck Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
