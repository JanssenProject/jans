package io.jans.agama.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.dsl.Transpiler;
import io.jans.agama.engine.continuation.PendingException;
import io.jans.agama.engine.continuation.PendingRedirectException;
import io.jans.agama.engine.continuation.PendingRenderException;
import io.jans.agama.engine.exception.FlowCrashException;
import io.jans.agama.engine.exception.FlowTimeoutException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.model.FlowResult;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.model.FlowMetadata;
import io.jans.agama.model.EngineConfig;
import io.jans.agama.model.Flow;
import io.jans.util.Pair;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.NativeJavaList;
import org.mozilla.javascript.NativeJavaMap;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;

@RequestScoped
public class FlowService {

    private static final String SESSION_ID_COOKIE = "session_id";
    private static final String SCRIPT_SUFFIX = ".js";
    
    private static final int TIMEOUT_SKEW = 8000; //millisecons
    
    @Inject
    private Logger logger;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private AgamaPersistenceService aps;
    
    @Inject
    private FlowUtils flowUtils;
    
    @Inject
    private EngineConfig engineConfig;
    
    @Inject
    private HttpServletRequest request;
    
    private String sessionId;
    private Context scriptCtx;
    private Scriptable globalScope;
    private Deque<Map<String, String>> parentsMappings;

    /**
     * Obtains the status of the current flow (if any) for the current user
     * @return
     * @throws IOException
     */
    public FlowStatus getRunningFlowStatus() throws IOException {
        return aps.getFlowStatus(sessionId);
    }

    public FlowStatus startFlow(FlowStatus status) throws FlowCrashException {
        
        try {
            status.setStartedAt(System.currentTimeMillis());
            String flowName = status.getQname();
            
            //retrieve the flow, execute until render/redirect is reached
            Flow flow = aps.getFlow(flowName, true);
            FlowMetadata fl = flow.getMetadata();
            String funcName = fl.getFuncName();

            verifyCode(flow);
            logger.info("Evaluating flow code");
            
            try {
                globalScope = initContext(scriptCtx);
                scriptCtx.evaluateString(globalScope, Transpiler.UTIL_SCRIPT_CONTENTS,
                        Transpiler.UTIL_SCRIPT_NAME, 1, null);
                flowUtils.printScopeIds(globalScope);
                
                scriptCtx.evaluateString(globalScope, flow.getTranspiled(), flowName + SCRIPT_SUFFIX, 1, null);
                flowUtils.printScopeIds(globalScope);

                logger.info("Executing function {}", funcName);
                Function f = (Function) globalScope.get(funcName, globalScope);
                parentsMappings = status.getParentsMappings();

                Object[] params = getFuncParams(fl, status.getJsonInput());
                Object val = scriptCtx.callFunctionWithContinuations(f, globalScope, params);
                NativeObject result = checkJSReturnedValue(val);                
                finishFlow(result, status);
                
            } catch (ContinuationPending pe) {
                status = processPause(pe, status);

            } catch (Exception e){
                terminateFlow();
                makeCrashException(e);
            }
            
            //TODO: review exception handling, enable polling if needed
        } catch (IOException ie) {
            throw new FlowCrashException(ie.getMessage(), ie);
        }
        return status;
        
    }
    
    public FlowStatus continueFlow(FlowStatus status, String jsonParameters, boolean callbackResume,
            String cancelUrl) throws FlowCrashException, FlowTimeoutException {

        try {            
            if (callbackResume) {
                //disable usage of callback endpoint
                status.setAllowCallbackResume(false);
                aps.persistFlowStatus(sessionId, status);
            }
            
            try {
                ensureTimeNotExceeded(status);

                Pair<Scriptable, NativeContinuation> pcont = aps.getContinuation(sessionId);
                globalScope = pcont.getFirst();
                flowUtils.printScopeIds(globalScope);

                logger.debug("Resuming flow");
                parentsMappings = status.getParentsMappings();

                Object val = scriptCtx.resumeContinuation(pcont.getSecond(), 
                        globalScope, new Pair<>(cancelUrl, jsonParameters));
                NativeObject result = checkJSReturnedValue(val);
                finishFlow(result, status);

            } catch (ContinuationPending pe) {
                status = processPause(pe, status);
                
            } catch (FlowTimeoutException te) {
                terminateFlow();
                throw te;
            } catch (Exception e) {
                terminateFlow();
                makeCrashException(e);
            }
        } catch (IOException ie) {
            throw new FlowCrashException(ie.getMessage(), ie);
        }
        return status;
        
    }
    
    // This is called in the middle of a cx.resumeContinuation invocation (see util.js#_flowCall)
    public Pair<Function, NativeJavaObject> prepareSubflow(String subflowName,
            Map<String, String> mappings) throws IOException {

        logger.debug("Template mappings of subflow {} are {}", subflowName, mappings);
        Flow flow = aps.getFlow(subflowName, false);
        FlowMetadata fl = flow.getMetadata();
        String funcName = fl.getFuncName();

        String flowCodeFileName = subflowName + SCRIPT_SUFFIX;
        //strangely, scriptCtx is a bit messed at this point so initialization is required again...
        initContext(scriptCtx);

        scriptCtx.evaluateString(globalScope, flow.getTranspiled(), flowCodeFileName, 1, null);
        flowUtils.printScopeIds(globalScope);

        logger.info("Appending function {} to scope", funcName);
        Function f = (Function) globalScope.get(funcName, globalScope);
        
        //This value is useful when saving the state, see method processPause
        parentsMappings.push(mappings);

        logger.info("Evaluating subflow code");
        Map<String, Object> configs = Optional.ofNullable(fl.getProperties()).orElse(Collections.emptyMap());
        return new Pair<>(f, wrapListOrMap(configs));
        
    }
    
    public void ensureTimeNotExceeded(FlowStatus flstatus) throws FlowTimeoutException {

        //Use some seconds to account for the potential time difference due to redirections: 
        //jython script -> agama, agama -> jython script. This helps agama flows to timeout
        //before the unauthenticated unused time
        if (System.currentTimeMillis() + TIMEOUT_SKEW > flstatus.getFinishBefore()) {
            throw new FlowTimeoutException("You have exceeded the amount of time required " + 
                    "to complete your authentication", flstatus.getQname());
            //"Your authentication attempt has run for more than " + time + " seconds"
        }

    }
    
    public void closeSubflow() throws IOException { 
        parentsMappings.pop();
    }
    
    public void terminateFlow() throws IOException {
        aps.terminateFlow(sessionId);
    }
    
    private void finishFlow(NativeObject result, FlowStatus status) throws IOException {

        FlowResult res = flowResultFrom(result);
        status.setResult(res);
        aps.finishFlow(sessionId, res);

    }
    
    private void verifyCode(Flow fl) throws IOException {
    
        String code = fl.getTranspiled();
        if (code == null) {
            String msg = "Source code of flow " + fl.getQname() + " ";
            msg += fl.getCodeError() == null ? "has not been parsed yet" : "has errors";
            throw new IOException(msg);
        }
        
        if (!Optional.ofNullable(engineConfig.getDisableTCHV()).orElse(false)) {

            String hash = fl.getTransHash();
            //null hash means the code is being regenerated in this moment
            if (hash != null && !flowUtils.hash(code).equals(hash))
                throw new IOException("Transpiled code seems to have been altered. " +
                        "Restore the code by increasing this flow's jansRevision attribute");
        }
        
    }

    private FlowStatus processPause(ContinuationPending pending, FlowStatus status)
            throws FlowCrashException, IOException {

        PendingException pe = null;
        if (pending instanceof PendingRenderException) {

            PendingRenderException pre = (PendingRenderException) pending;
            String templPath = computeTemplatePath(pre.getTemplatePath(), parentsMappings);
            
            if (!templPath.contains("."))
                throw new FlowCrashException(
                        "Expecting file extension for the template to render: " + templPath);

            status.setTemplatePath(templPath);
            status.setTemplateDataModel(pre.getDataModel());
            status.setExternalRedirectUrl(null);
            pe = pre;

        } else if (pending instanceof PendingRedirectException) {
            
            PendingRedirectException pre = (PendingRedirectException) pending;
            
            status.setTemplatePath(null);
            status.setTemplateDataModel(null);
            status.setExternalRedirectUrl(pre.getLocation());
            pe = pre;
            
        } else {
            throw new IllegalArgumentException("Unexpected instance of ContinuationPending");
        }

        logger.debug("Parents mappings: {}", status.getParentsMappings());
        status.setAllowCallbackResume(pe.isAllowCallbackResume());
        //Save the state
        aps.saveState(sessionId, status, pe.getContinuation(), globalScope);

        return status;
        
    }
    
    private String computeTemplatePath(String path, Deque<Map<String, String>> parentsMappings) {

        String result = path;
        for (Map<String, String> mapping : parentsMappings) {
            String overriden = mapping.get(result);
            if (overriden != null) result = overriden;
        }

        logger.info("Inferred template for {} is {}", path, result);
        return result;

    }
    
    private Object[] getFuncParams(FlowMetadata metadata, String strParams) throws JsonProcessingException {
        
        List<String> inputs = Optional.ofNullable(metadata.getInputs()).orElse(Collections.emptyList());
        Map<String, Object> configs = Optional.ofNullable(metadata.getProperties()).orElse(Collections.emptyMap());

        Object[] params = new Object[inputs.size() + 1];
        params[0] = wrapListOrMap(configs);
        
        if (strParams != null) {
            Map<String, Object> map = mapper.readValue(strParams, new TypeReference<Map<String, Object>>(){});
            for (int i = 1; i < params.length; i++) {
                params[i] = map.get(inputs.get(i - 1));
            }
        }
        for (int i = 1; i < params.length; i++) {
            String input = inputs.get(i - 1);

            if (params[i] == null) {
                logger.warn("Setting parameter '{}' to null", input);                
            } else {
                logger.debug("Setting parameter '{}' to an instance of {}", input, params[i].getClass().getName());

                NativeJavaObject wrapped = wrapListOrMap(params[i]); 
                params[i] = wrapped == null ? params[i] : wrapped;
            }
        }
        return params;

    }
    
    private NativeJavaObject wrapListOrMap(Object obj) {

        //This helps prevent exception "Invalid JavaScript value of type ..."
        //when typeof is applied over this param in JavaScript code
        if (Map.class.isInstance(obj)) {
            return new NativeJavaMap(globalScope, obj);
        } else if (List.class.isInstance(obj)) {
            return new NativeJavaList(globalScope, obj);                    
        }
        return null;
        
    }
    
    private NativeObject checkJSReturnedValue(Object obj) throws Exception {

        try {
            //obj is not null
            return NativeObject.class.cast(obj);
        } catch (ClassCastException e) {
            if (Undefined.isUndefined(obj)) {
                throw new Exception("No Finish instruction was reached");
            } else throw e;
        }

    }

    private void makeCrashException(Exception e) throws FlowCrashException {

        String msg;
        if (e instanceof RhinoException) {            
            RhinoException re = (RhinoException) e;
            msg = re.details();
            logger.error(msg + re.getScriptStackTrace());
            //logger.error(re.getMessage());
            msg = "Error executing flow's code - " + msg;
        } else 
            msg = e.getMessage();
        
        throw new FlowCrashException(msg, e);
        
    }

    /**
     * @param result
     * @return 
     * @throws JsonProcessingException
     */
    private FlowResult flowResultFrom(NativeObject result) throws JsonProcessingException {        
        return mapper.convertValue(result, FlowResult.class);
    }

    private Scriptable initContext(Context ctx) {
        ctx.setLanguageVersion(Context.VERSION_ES6);
        ctx.setOptimizationLevel(-1);
        return ctx.initStandardObjects();
    }
    
    @PostConstruct
    private void init() {
        
        class AgamaContextFactory extends ContextFactory {
            
            @Override
            protected boolean hasFeature(Context cx, int featureIndex) {
                switch (featureIndex) {
                    case Context.FEATURE_ENABLE_JAVA_MAP_ACCESS: return true;
                }
                return super.hasFeature(cx, featureIndex);
            }
        }
        
        scriptCtx = new AgamaContextFactory().enterContext();        
        sessionId = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            sessionId = Stream.of(cookies).filter(coo -> coo.getName().equals(SESSION_ID_COOKIE))
                .findFirst().map(Cookie::getValue).orElse(null);
        }
        if (sessionId == null) { 
            logger.warn("Session ID not found");
        }

    }

    @PreDestroy
    private void finish() {
        Context.exit();
    }

}
