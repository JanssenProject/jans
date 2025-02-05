import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.AuthorizationChallengeSession;
import io.jans.as.server.authorize.ws.rs.AuthorizationChallengeSessionService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzchallenge.AuthorizationChallengeType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.agama.engine.model.*;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.service.AgamaPersistenceService;
import io.jans.agama.NativeJansFlowBridge;
import io.jans.agama.engine.client.MiniBrowser;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.util.*;

import jakarta.servlet.ServletRequest;
import java.io.IOException;
import java.util.*;

import org.json.*;

import static io.jans.agama.engine.client.MiniBrowser.Outcome.*;

public class AuthorizationChallenge implements AuthorizationChallengeType {

    //private static final Logger log = LoggerFactory.getLogger(AuthorizationChallenge.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    private String finishIdAttr;
    private MiniBrowser miniBrowser;
    private PersistenceEntryManager entryManager;
    private AuthorizationChallengeSessionService deviceSessionService;

    private boolean makeError(ExternalScriptContext context, AuthorizationChallengeSession deviceSessionObject,
                boolean doRemoval, String errorId, JSONObject error, int status) {

        JSONObject jobj = new JSONObject();
        if (deviceSessionObject != null) {
            
            if (doRemoval) {
                entryManager.remove(deviceSessionObject.getDn(), AuthorizationChallengeSession.class);
            } else {
                jobj.put("auth_session", deviceSessionObject.getId());
            }
        }

        String errId = errorId.toLowerCase();
        jobj.put("error", errId);
        jobj.put(errId, error);

        context.createWebApplicationException(status, jobj.toString(2) + "\n");
        return false;

    }
    
    private boolean makeUnexpectedError(ExternalScriptContext context, AuthorizationChallengeSession deviceSessionObject,
                String description) {
    
        JSONObject jobj = new JSONObject(Map.of("description", description));
        return makeError(context, deviceSessionObject, true, "unexpected_error", jobj, 500);

    }
    
    private boolean makeMissingParamError(ExternalScriptContext context, String description) {
        
        JSONObject jobj = new JSONObject(Map.of("description", description));
        return makeError(context, null, false, "missing_param", jobj, 400);

    }

    private Pair<String, String> prepareFlow(String sessionId, String flowName) {
        
        String msg = null;
        try {
            String qn = null, inputs = null;
            
            int i = flowName.indexOf("-");
            if (i == -1) {
                qn = flowName;
            } else if (i == 0) {
                msg = "Flow name is empty";
            } else {
                qn = flowName.substring(0, i);
                scriptLogger.info("Parsing flow inputs");
                inputs = Base64Util.base64urldecodeToString(flowName.substring(i + 1));
            }
            
            if (qn != null) {
                NativeJansFlowBridge bridge = CdiUtil.bean(NativeJansFlowBridge.class);            
                Boolean running = bridge.prepareFlow(sessionId, qn, inputs, true, null);
    
                if (running == null) {
                    msg = "Flow " + qn + " does not exist or cannot be launched from an application";
                } else if (running) {
                    msg = "Flow is already in course";
                } else {
                    return new Pair<>(bridge.getTriggerUrl(), null);
                }
            }
            
        } catch (Exception e) {
            msg = e.getMessage();
            scriptLogger.error(msg, e);
        }
        return new Pair<>(null, msg);
        
    }
    
    private User extractUser(String userId) {
                    
        UserService userService = CdiUtil.bean(UserService.class);
        List<User> matchingUsers = userService.getUsersByAttribute(finishIdAttr, userId, true, 2);
        int matches = matchingUsers.size();
        
        if (matches != 1) {
            if (matches == 0) {
                scriptLogger.warn("No user matches the required condition: {}={}", finishIdAttr, userId);
            } else {
                scriptLogger.warn("Several users match the required condition: {}={}", finishIdAttr, userId);
            }
            
            return null;
        }
        return matchingUsers.get(0);
        
    }

    @Override
    public boolean authorize(Object scriptContext) {
        
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        
        if (!CdiUtil.bean(FlowUtils.class).serviceEnabled())
            return makeUnexpectedError(context, null, "Agama engine is disabled");   

        AuthzRequest authRequest = context.getAuthzRequest();
        
        if (!authRequest.isUseAuthorizationChallengeSession())            
            return makeMissingParamError(context, "Please set 'use_auth_session=true' in your request");
        
        ServletRequest servletRequest = context.getHttpRequest();
        AuthorizationChallengeSession deviceSessionObject = authRequest.getAuthorizationChallengeSessionObject();

        boolean noSO = deviceSessionObject == null;
        scriptLogger.debug("There IS{} device session object", noSO ? " NO" : "");
        
        Map<String, String> deviceSessionObjectAttrs = null;
        String sessionId = null, url = null, payload = null;
        
        if (noSO) {
            
            String fname = servletRequest.getParameter("flow_name");            
            if (fname == null)
                return makeMissingParamError(context, "Parameter 'flow_name' missing in request");           
            
            deviceSessionObject = deviceSessionService.newAuthorizationChallengeSession();
            sessionId = deviceSessionObject.getId();

            Pair<String, String> pre = prepareFlow(sessionId, fname);
            url = pre.getFirst();

            if (url == null) return makeUnexpectedError(context, deviceSessionObject, pre.getSecond());
            
            deviceSessionObjectAttrs = deviceSessionObject.getAttributes().getAttributes();
            deviceSessionObjectAttrs.put("url", url);
            deviceSessionObjectAttrs.put("client_id", servletRequest.getParameter("client_id"));
            deviceSessionObjectAttrs.put("acr_values", servletRequest.getParameter("acr_values"));
            deviceSessionObjectAttrs.put("scope", servletRequest.getParameter("scope"));
            
            deviceSessionService.persist(deviceSessionObject);

            authRequest.setAuthorizationChallengeSessionObject(deviceSessionObject);
            authRequest.setAuthorizationChallengeSession(deviceSessionObject.getId());
            
        } else {
            sessionId = deviceSessionObject.getId();
            deviceSessionObjectAttrs = deviceSessionObject.getAttributes().getAttributes();
            String userId = deviceSessionObjectAttrs.get("userId");

            if (userId != null) {
                User user = extractUser(userId);

                if (user == null)
                    return makeUnexpectedError(context, deviceSessionObject, "Unable to determine identity of user");

                context.getExecutionContext().setUser(user);
                scriptLogger.debug("User {} is authenticated successfully", user.getUserId());

                entryManager.remove(deviceSessionObject.getDn(), AuthorizationChallengeSession.class);
                return true;
            }
            
            url = deviceSessionObjectAttrs.get("url");                
            if (url == null)
                return makeUnexpectedError(context, deviceSessionObject, "Illegal state - url is missing in device session object");
            
            payload = servletRequest.getParameter("data");
            if (payload == null)
                return makeMissingParamError(context, "Parameter 'data' missing in request");
        }
        
        Pair<MiniBrowser.Outcome, JSONObject> p = miniBrowser.move(sessionId, url, payload);            
        MiniBrowser.Outcome result = p.getFirst();
        String strRes = result.toString();
        JSONObject jres = p.getSecond();
        
        if (result == CLIENT_ERROR || result == ENGINE_ERROR) {
            return makeError(context, deviceSessionObject, true, strRes, jres, 500);

        } else if (result == FLOW_PAUSED){
            url = p.getSecond().remove(MiniBrowser.FLOW_PAUSED_URL_KEY).toString();
            deviceSessionObjectAttrs.put("url", url);
            deviceSessionService.merge(deviceSessionObject);
            
            scriptLogger.info("Next url will be {}", url);
            return makeError(context, deviceSessionObject, false, strRes, jres, 401);
            
        } else if (result == FLOW_FINISHED) {
            
            try {
                AgamaPersistenceService aps = CdiUtil.bean(AgamaPersistenceService.class);
                FlowStatus fs = aps.getFlowStatus(sessionId);
                
                if (fs == null) 
                    return makeUnexpectedError(context, deviceSessionObject, "Flow is not running");
            
                FlowResult fr = fs.getResult();                
                if (fr == null)
                    return makeUnexpectedError(context, deviceSessionObject,
                                "The flow finished but the resulting outcome was not found");
                
                JSONObject jobj = new JSONObject(fr);
                jobj.remove("aborted");     //just to avoid confusions and questions from users
                
                if (!fr.isSuccess()) {
                    scriptLogger.info("Flow DID NOT finished successfully");
                    return makeError(context, deviceSessionObject, true, strRes, jobj, 401);
                }

                String userId = Optional.ofNullable(fr.getData()).map(d -> d.get("userId"))
                        .map(Object::toString).orElse(null);
                            
                if (userId == null)
                    return makeUnexpectedError(context, deviceSessionObject, "Unable to determine identity of user. " +
                            "No userId provided in flow result");

                deviceSessionObjectAttrs.put("userId", userId);
                deviceSessionService.merge(deviceSessionObject);
                aps.terminateFlow(sessionId);
                
                return makeError(context, deviceSessionObject, false, strRes, jobj, 401);
                    
            } catch (IOException e) {
                return makeUnexpectedError(context, deviceSessionObject, e.getMessage());
            }
        } else {
            return makeUnexpectedError(context, deviceSessionObject, "Illegal state - unexpected outcome " + strRes);
        }

    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized Agama AuthorizationChallenge Java custom script");        
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {   
        
        scriptLogger.info("Initialized Agama AuthorizationChallenge Java custom script.");
        finishIdAttr = null;
        String name = "finish_userid_db_attribute";
        SimpleCustomProperty prop = configurationAttributes.get(name);
        
        if (prop != null) {
            finishIdAttr = prop.getValue2();
            if (StringHelper.isEmpty(finishIdAttr)) {
                finishIdAttr = null;
            }
        }

        if (finishIdAttr == null) {
            scriptLogger.info("Property '{}' is missing value", name);
            return false;
        }
        scriptLogger.info("DB attribute '{}' will be used to map the identity of userId passed "+
            "in Finish directives (if any)", finishIdAttr);

        entryManager = CdiUtil.bean(PersistenceEntryManager.class);
        deviceSessionService = CdiUtil.bean(AuthorizationChallengeSessionService.class);
        miniBrowser = new MiniBrowser(CdiUtil.bean(AppConfiguration.class).getIssuer());
        return true;

    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed Agama AuthorizationChallenge Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
    
    @Override
    public Map<String, String> getAuthenticationMethodClaims(Object context) {
        return Map.of();
    }

    @Override
    public void prepareAuthzRequest(Object scriptContext) {
        
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        AuthzRequest authRequest = context.getAuthzRequest();

        AuthorizationChallengeSession sessionObject = authRequest.getAuthorizationChallengeSessionObject();
        if (sessionObject != null) {
            Map<String, String> sessionAttributes = sessionObject.getAttributes().getAttributes();

            // set scope from session into request object
            String scopeFromSession = sessionAttributes.get("scope");
            if (StringUtils.isNotBlank(scopeFromSession) && StringUtils.isBlank(authRequest.getScope())) {
                authRequest.setScope(scopeFromSession);
            }
        }
    }

}
