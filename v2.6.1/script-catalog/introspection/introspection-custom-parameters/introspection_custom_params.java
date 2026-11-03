import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.introspection.IntrospectionType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.SessionIdService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.server.model.common.SessionId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.util.Map;


public class Introspection implements IntrospectionType {

    private static final Logger log = LoggerFactory.getLogger(Introspection.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Introspection Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Introspection Java custom script");
        scriptLogger.info("Introspection Java script. Initializing ...");
        scriptLogger.info("Introspection Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of Introspection Java custom script");
        scriptLogger.info("Introspection Java script. Destroying ...");
        scriptLogger.info("Introspection Java script. Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    // Returns boolean, true - apply introspection method, false - ignore it.
    // This method is called after introspection response is ready. This method can modify introspection response.
    // Note :
    // responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    // context is reference of io.jans.as.server.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenProject project, )

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {

        JSONObject response = (JSONObject) responseAsJsonObject;
        ExternalIntrospectionContext ctx = (ExternalIntrospectionContext) context;

        response.accumulate("key_from_java", "value_from_script_on_java");

        AuthorizationGrant authorizationGrant = ctx.getTokenGrant();
        if(authorizationGrant == null) {
            scriptLogger.info("Introspection Java script. Failed to load authorization grant by context");
            return false;
        }
        
        // Put user_id into response
        response.accumulate("user_id", authorizationGrant.getUser().getUserId()); 

        // Put custom parameters into response
        String sessionDn = authorizationGrant.getSessionDn();
        if(sessionDn == null) {
            // There is no session
            scriptLogger.info("Introspection Java script. Failed to load session DN");
            return true;
        }

        // Return session_id
        response.accumulate("session_id", sessionDn);

        SessionIdService sessionIdService = CdiUtil.bean(SessionIdService.class);
        SessionId session = sessionIdService.getSessionById(sessionDn, false);
        if(session == null) {
            scriptLogger.info("Introspection Java script. Failed to load session");
            return true;
        }

        Map<String, String> sessionAttributes = session.getSessionAttributes();
        if(sessionAttributes == null) {
            // There is no session attributes
            return true;
        }

        // Append custom claims
        if(sessionAttributes.containsKey("custom1")) {
            response.accumulate("custom1", sessionAttributes.get("custom1"));
        }
        if(sessionAttributes.containsKey("custom2")) {
            response.accumulate("custom2", sessionAttributes.get("custom2"));
        }
        
        return true;
    }
}
