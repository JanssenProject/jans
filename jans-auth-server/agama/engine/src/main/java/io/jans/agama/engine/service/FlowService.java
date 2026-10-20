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
import io.jans.agama.engine.script.ScriptUtils;
import io.jans.agama.engine.service.TrackingService;
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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.mozilla.javascript.*;
import org.slf4j.Logger;

@RequestScoped
public class FlowService {

    private static final String SESSION_ID_COOKIE = "session_id";
    private static final String SCRIPT_SUFFIX = ".js";
    
    private static final int TIMEOUT_SKEW = 8000; //milliseconds
    
    @Inject
    private Logger logger;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private AgamaPersistenceService aps;

    @Inject
    private TrackingService tracker;

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

    private class JSValueInspector {       
        
        private boolean undefined;
        private NativeObject nobj;
        private Exception ex;
        
        JSValueInspector(Object obj) {
            
            undefined = obj != null && Undefined.isUndefined(obj);            
            if (!undefined) {
                
                try {
                    nobj = NativeObject.class.cast(obj);
                } catch (ClassCastException e) {
                    try {
                        ex = Exception.class.cast(NativeJavaObject.class.cast(obj).unwrap());
                    } catch (ClassCastException e2) { }
                }
            }           
            
        }
        
        boolean isUndefined() {
            return undefined;
        }
        
        NativeObject asNativeObject() {
            return nobj;            
        }
        
        Exception asException() {
            return ex;
        }
        
    }
    
    /**
     * Obtains the status of the current flow (if any) for the current user
     * @return
     * @throws IOException
     */
    public FlowStatus getRunningFlowStatus() throws IOException {
        return aps.getFlowStatus(sessionId);
    }

    public FlowStatus startFlow(FlowStatus status) throws FlowCrashException {
        
        status.setStartedAt(System.currentTimeMillis());
        String flowName = status.getQname();
        Object val = null;
        
        try {
            //retrieve the flow, execute until render or redirect is reached
            Flow flow = aps.getFlow(flowName, true);
            FlowMetadata fl = flow.getMetadata();
            String funcName = fl.getFuncName();

            verifyCode(flow);
            logger.info("Evaluating flow code");
            
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
            val = scriptCtx.callFunctionWithContinuations(f, globalScope, params);
            
        } catch (ContinuationPending pe) {
            return processPause(pe, status);

        } catch (IOException ie) {
            reportAndThrow(ie, flowName, 0);
        }

        wrapUpExecution(flowName, status, val);
        return status;
        
    }
    
    public FlowStatus continueFlow(FlowStatus status, String jsonParameters, boolean callbackResume,
            String cancelUrl) throws FlowCrashException, FlowTimeoutException {

        String flowName = status.getQname();
        Object val = null;    
        try {            
            if (callbackResume) {
                //disable usage of callback endpoint
                status.setAllowCallbackResume(false);
                aps.persistFlowStatus(sessionId, status);
            }
            ensureTimeNotExceeded(status);

            Pair<Scriptable, NativeContinuation> pcont = aps.getContinuation(sessionId);
            globalScope = pcont.getFirst();
            flowUtils.printScopeIds(globalScope);

            logger.debug("Resuming flow");
            parentsMappings = status.getParentsMappings();

            val = scriptCtx.resumeContinuation(pcont.getSecond(), globalScope,
                    new Pair<>(cancelUrl, jsonParameters));

        } catch (ContinuationPending pe) {
            return processPause(pe, status);

        } catch (IOException ie) {
            reportAndThrow(ie, flowName, 0);
        }
        
        wrapUpExecution(flowName, status, val);
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

            String flowName = flstatus.getQname();
            FlowTimeoutException te = new FlowTimeoutException("You have exceeded the amount of " + 
                    "time required to complete your authentication", flowName);
            //"Your authentication attempt has run for more than " + time + " seconds"
            terminateFlowWithReport(flowName);
            reportEx(te, flowName, 0);
            throw te;
        }

    }
    
    public void closeSubflow() throws IOException { 
        parentsMappings.pop();
    }
    
    public void terminateFlow() throws IOException {
        aps.terminateFlow(sessionId);
    }
    
    public void terminateFlowWithReport(String flowQname) {
        
        try {
            terminateFlow();
        } catch (IOException ie) {
            reportEx(ie, flowQname, 0);
        }

    }

    private void reportEx(Exception e, String flowQname, int line) {
        tracker.track(flowQname, line, e);
    }

    private void reportAndThrow(Exception e, String flowQname, int line) throws FlowCrashException {
        reportEx(e, flowQname, line);
        throwFlowCrashEx(e);
    }

    private void wrapUpExecution(String flowQname, FlowStatus status, Object val)
            throws FlowCrashException {

        Exception ex = null;
        JSValueInspector insp = new JSValueInspector(val);
        NativeObject result = insp.asNativeObject();
       
        if (result == null) {
            ex = insp.asException(); //When non-null, the exception was already reported from JS code

            if (ex == null) {
                ex = new Exception(insp.isUndefined() ?
                        //Truthy value may only arise for top-level flows. For subflows see util.js#_flowCall
                        "No Finish instruction was reached during execution of flow" :
                        "Unexpected value received upon flow termination");
                reportEx(ex, flowQname, 0);
            }
        }

        if (ex != null) {
            terminateFlowWithReport(flowQname);
        } else {
            try {
                finishFlow(result, status);
            } catch (IOException e) {
                reportEx(e, flowQname, 0);
                ex = e;
            }
        }

        if (ex != null) throwFlowCrashEx(ex);

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
            throws FlowCrashException {

        PendingException pe = (PendingException) pending;
        String currQname = pe.getCurrentQname();
        int currLine = pe.getCurrentLine();

        try {
            if (pending instanceof PendingRenderException) {
    
                PendingRenderException pre = (PendingRenderException) pending;
                
                if (!ScriptUtils.isValidDataModel(pre.getDataModel()))
                    throw new IllegalArgumentException("Data passed to RRF was not a map or Java equivalent");
                
                String templPath = computeTemplatePath(ScriptUtils.normalize(pre.getTemplatePath()),
                        parentsMappings);
                
                if (!templPath.contains("."))
                    throw new IOException(
                            "Expecting file extension for the template to render: " + templPath);
//TODO: track qname + templPath count
                status.setTemplatePath(templPath);
                status.setTemplateDataModel(pre.getDataModel());
                status.setExternalRedirectUrl(null);
    
            } else if (pending instanceof PendingRedirectException) {
                
                if (status.isNativeClient())
                    throw new IOException("RFAC for native clients is not available");

                PendingRedirectException pre = (PendingRedirectException) pending;
                
                status.setTemplatePath(null);
                status.setTemplateDataModel(null);
                status.setExternalRedirectUrl(pre.getLocation());
                
            }
            logger.debug("Parents mappings: {}", status.getParentsMappings());
            status.setAllowCallbackResume(pe.isAllowCallbackResume());
            status.setCurrentQname(currQname);
            status.setCurrentLine(currLine);
            
            //Save the state
            aps.saveState(sessionId, status, pe.getContinuation(), globalScope);
        } catch (Exception e) {
            terminateFlowWithReport(status.getQname());
            reportAndThrow(e, currQname, currLine);
        }
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

    private void throwFlowCrashEx(Exception e) throws FlowCrashException {
        throw new FlowCrashException(e.getMessage(), e);        
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
