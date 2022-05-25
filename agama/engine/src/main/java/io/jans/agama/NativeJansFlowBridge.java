package io.jans.agama;

import io.jans.agama.engine.model.FlowResult;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.engine.service.AgamaPersistenceService;
import io.jans.agama.engine.service.FlowService;
import io.jans.agama.engine.service.WebContext;
import io.jans.agama.engine.servlet.ExecutionServlet;
import io.jans.agama.engine.script.LogUtils;
import io.jans.agama.model.EngineConfig;

import jakarta.inject.Inject;
import jakarta.enterprise.context.RequestScoped;
import java.io.IOException;

import org.slf4j.Logger;

@RequestScoped
public class NativeJansFlowBridge {
            
    @Inject
    private Logger logger;
    
    @Inject
    private AgamaPersistenceService aps;
    
    @Inject
    private FlowService fs;
    
    @Inject
    private EngineConfig conf;
    
    @Inject
    private WebContext webContext;
    
    public String scriptPageUrl() {
        return conf.getBridgeScriptPage();
    }

    public String getTriggerUrl() {
        return webContext.getContextPath() + ExecutionServlet.URL_PREFIX + 
                "agama" + ExecutionServlet.URL_SUFFIX;       
    }
    
    public Boolean prepareFlow(String sessionId, String qname, String jsonInput) throws Exception {
        
        logger.info("Preparing flow '{}'", qname);
        Boolean alreadyRunning = null;
        if (aps.flowEnabled(qname)) {
            
            FlowStatus st = aps.getFlowStatus(sessionId);
            alreadyRunning = st != null;
            
            if (alreadyRunning && !st.getQname().equals(qname)) {
                logger.warn("Flow {} is already running. Will be terminated", st.getQname());
                fs.terminateFlow();
                st = null;
            }
            if (st == null) {

                int timeout = aps.getEffectiveFlowTimeout(qname);
                if (timeout <= 0) throw new Exception("Flow timeout negative or zero. " +
                        "Check your AS configuration or flow definition");                
                long expireAt = System.currentTimeMillis() + 1000L * timeout;                

                st = new FlowStatus();
                st.setStartedAt(FlowStatus.PREPARED);
                st.setQname(qname);
                st.setJsonInput(jsonInput);
                st.setFinishBefore(expireAt);
                aps.createFlowRun(sessionId, st, expireAt);
                LogUtils.log("@w Effective timeout for this flow will be % seconds", timeout);
            }
        }        
        return alreadyRunning;

    }
    
    public FlowResult close() throws IOException {
        
        FlowStatus st = fs.getRunningFlowStatus();
        if (st == null) {
            logger.error("No current flow running");
            
        } else if (st.getStartedAt() != FlowStatus.FINISHED) {
            logger.error("Current flow hasn't finished");
            
        } else {
            fs.terminateFlow();
            return st.getResult();
        }
        return null;
        
    }
    
}
