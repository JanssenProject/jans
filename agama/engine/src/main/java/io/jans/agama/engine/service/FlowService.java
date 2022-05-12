package io.jans.agama.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.engine.continuation.PendingException;
import io.jans.agama.engine.continuation.PendingRedirectException;
import io.jans.agama.engine.continuation.PendingRenderException;
import io.jans.agama.engine.exception.FlowCrashException;
import io.jans.agama.engine.exception.FlowTimeoutException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.model.FlowResult;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.engine.model.ParentFlowData;
import io.jans.agama.model.FlowMetadata;
import io.jans.agama.model.Config;
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
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;

@RequestScoped
public class FlowService {

    private static final String SESSION_ID_COOKIE = "session_id";
    private static final String SCRIPT_SUFFIX = ".js";
    private static final String JS_UTIL = "util.js";
    
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
    private Config config;
    
    @Inject
    private HttpServletRequest request;
    
    private String sessionId;
    private Context scriptCtx;
    private Scriptable globalScope;
    private ParentFlowData parentFlowData;

    /**
     * Obtains the status of the current flow (if any) for the current user
     * @return
     * @throws IOException
     */
    public FlowStatus getRunningFlowStatus() throws IOException {
        return aps.getFlowStatus(sessionId);
    }

    public boolean isEnabled(String flowName) {        
        return aps.flowEnabled(flowName);
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
                scriptCtx.evaluateString(globalScope, config.getUtilScript(), JS_UTIL, 1, null);
                flowUtils.printScopeIds(globalScope);
                
                scriptCtx.evaluateString(globalScope, flow.getTranspiled(), flowName + SCRIPT_SUFFIX, 1, null);
                flowUtils.printScopeIds(globalScope);

                logger.info("Executing function {}", funcName);
                Function f = (Function) globalScope.get(funcName, globalScope);

                Object[] params = getFlowParams(fl.getInputs(), status.getJsonInput());
                NativeObject result = (NativeObject) scriptCtx.callFunctionWithContinuations(f, globalScope, params);                
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
            boolean abortSubflow) throws FlowCrashException, FlowTimeoutException {

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
                parentFlowData = status.getParentsData().peekLast();

                NativeObject result = (NativeObject) scriptCtx.resumeContinuation(pcont.getSecond(), 
                        globalScope, new Pair<>(abortSubflow, jsonParameters));
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
    public Function prepareSubflow(String subflowName, String parentBasepath, String[] pathOverrides)
            throws IOException {
        
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
        //The values set below are useful when saving the state, see method processPause
        
        ParentFlowData pfd = new ParentFlowData();        
        pfd.setParentBasepath(parentBasepath);
        pfd.setPathOverrides(pathOverrides);
        parentFlowData = pfd;

        logger.info("Evaluating subflow code");
        return f;
        
    }
    
    public void ensureTimeNotExceeded(FlowStatus flstatus) throws FlowTimeoutException {

        int time = config.getEngineConf().getInterruptionTime();
        //Use some seconds to account for the potential time difference due to redirections: 
        //jython script -> agama, agama -> jython script. This helps agama flows to timeout
        //before the unauthenticated unused time
        if (time > 0 && 
                System.currentTimeMillis() - flstatus.getStartedAt() + TIMEOUT_SKEW > 1000 * time) {

            throw new FlowTimeoutException("You have exceeded the amount of time required " + 
                    "to complete your authentication process", flstatus.getQname());
            //"Your authentication attempt has run for more than " + time + " seconds"
        }

    }
    
    public void closeSubflow() throws IOException {
        parentFlowData = null;
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
            String msg = "Source code of flow " + fl.getQName() + " ";
            msg += fl.getCodeError() == null ? "has not been parsed yet" : "has errors";
            throw new IOException(msg);
        }
        
        if (Optional.ofNullable(config.getEngineConf().getDisableTCHV()).orElse(false)) {

            String hash = fl.getTransHash();
            //null hash means the code is being regenerated in this moment

            if (hash != null && !flowUtils.hash(code).equals(hash))
                throw new IOException("Flow code seems to have been altered. " +
                        "Restore the code by increasing this flow's jansRevision attribute");
        }
        
    }

    private FlowStatus processPause(ContinuationPending pending, FlowStatus status)
            throws FlowCrashException, IOException {
        
        PendingException pe = null;
        if (pending instanceof PendingRenderException) {

            PendingRenderException pre = (PendingRenderException) pending;
            String templPath = pre.getTemplatePath();
            
            if (!templPath.contains("."))
                throw new FlowCrashException(
                        "Expecting file extension for the template to render: " + templPath);

            status.setTemplatePath(computeTemplatePath(templPath, parentFlowData));
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
        
        if (parentFlowData == null) {
            status.getParentsData().pollLast();
        } else {
            status.getParentsData().offer(parentFlowData);
        }

        status.setAllowCallbackResume(pe.isAllowCallbackResume());
        //Save the state
        aps.saveState(sessionId, status, pe.getContinuation(), globalScope);

        return status;
        
    }
    
    private String computeTemplatePath(String path, ParentFlowData pfd) {
        
        String[] overrides = Optional.ofNullable(pfd).map(ParentFlowData::getPathOverrides)
                .orElse(new String[0]);

        if (Stream.of(overrides).anyMatch(path::equals))
            return pfd.getParentBasepath() + "/" + path;
        return path;

    }
    
    private Object[] getFlowParams(List<String> inputNames, String strParams) throws JsonProcessingException {

        List<String> inputs = Optional.ofNullable(inputNames).orElse(Collections.emptyList());
        Object[] params = new Object[inputs.size()];

        if (strParams != null) {
            Map<String, Object> map = mapper.readValue(strParams, new TypeReference<Map<String, Object>>(){});
            for (int i = 0; i < params.length; i++) {
                params[i] = map.get(inputs.get(i));
            }
        }
        for (int i = 0; i < params.length; i++) {
            String input = inputs.get(i);

            if (params[i] == null) {
                logger.warn("Setting parameter '{}' to null", input);                
            } else {
                logger.debug("Setting parameter '{}' to an instance of {}", input, params[i].getClass().getName());

                //This helps prevent exception "Invalid JavaScript value of type ..."
                //when typeof is applied over this param in JavaScript code
                if (Map.class.isInstance(params[i])) {
                    params[i] = new NativeJavaMap(globalScope, params[i]);
                } else if (List.class.isInstance(params[i])) {
                    params[i] = new NativeJavaList(globalScope, params[i]);                    
                }
            }
        }
        return params;

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
    public FlowResult flowResultFrom(NativeObject result) throws JsonProcessingException {        
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
