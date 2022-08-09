/*
 * Copyright (c) 2022, Janssen Project
 * 
 * Author: Gluu
 *    1. Filter out a value
 *    2. Add a value
 *    3. Get ip address of the client making the request
 */

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.model.common.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.util.Map;


public class Discovery implements DiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(Discovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        scriptLogger.info("Discovery Java script. Initializing ...");
        scriptLogger.info("Discovery Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of Discovery Java custom script");
        scriptLogger.info("Discovery Java script. Destroying ...");
        scriptLogger.info("Discovery Java script. Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    // Returns boolean, true - apply Discovery method, false - ignore it.
    // This method is called after Discovery response is ready. This method can modify Discovery response.
    // Note :
    // responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    // context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject project, )

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {

        scriptLogger.info("Custom Java - Inside modifyResponse method of Discovery script ....");

        JSONObject response = (JSONObject) responseAsJsonObject;
        ExecutionContext ctx = (ExecutionContext) context;

        // Add a value to Discovery Response
        response.accumulate("key_from_java", "value_from_script_on_java");

        // Filter out a value from Discovery Response
        response.remove("pushed_authorization_request_endpoint");

        // Get an IP Address of the Client making the request
        response.accumulate("Client IP Address", ctx.getHttpRequest().getHeader("X-Forwarded-For"));
        
        return true;
    }
}
