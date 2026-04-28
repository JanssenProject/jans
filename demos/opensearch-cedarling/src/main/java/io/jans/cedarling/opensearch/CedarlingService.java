package io.jans.cedarling.opensearch;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;

import java.util.*;

import org.apache.logging.log4j.*;
import org.json.JSONObject;

import uniffi.cedarling_uniffi.*;

public class CedarlingService {
    
    private CedarlingAdapter cedarlingAdapter;    
    private Logger logger = LogManager.getLogger(getClass());
    private boolean started;
    private boolean useLogging;
    
    private static CedarlingService instance = new CedarlingService();
    
    private CedarlingService() {
        cedarlingAdapter = new CedarlingAdapter();
    }

    public static CedarlingService getInstance() {
        return instance;
    }
    
    public void init(JSONObject bootstrapProperties, boolean useLogging) {
        
        try {
            started = false;
            logger.info("Initializing Cedarling...");
            cedarlingAdapter.loadFromJson(bootstrapProperties.toString());

            if (useLogging) { 
                List<String> initLogs = cedarlingAdapter.getLogsByTag("System");
                initLogs.forEach(line -> logger.debug("   {}", line));
            }
            
            started = true;
            this.useLogging = useLogging;
            logger.info("Done");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }        
        
    }
    
    public boolean isStarted() {
        return started;
    }

    public boolean authorize(Map<String, String> tokens, String action, Map<String, Object> resource,
            JSONObject context) throws Exception {

        List<TokenInput> tokenInputs = new ArrayList<>();
        tokens.entrySet().forEach(e -> tokenInputs.add(new TokenInput(e.getKey(), e.getValue())));        
            
        MultiIssuerAuthorizeResult res = cedarlingAdapter.authorizeMultiIssuer(tokenInputs, action,
                new JSONObject(resource), context);        
        boolean authorized = res.getDecision();
        
        if (!authorized && useLogging) {
            List<String> decisionLogs = cedarlingAdapter.getLogsByRequestId(res.getRequestId());
            
            logger.debug("Unauthorized decision{}", decisionLogs.isEmpty() ? ". No logs available" : "");
            decisionLogs.forEach(line -> logger.trace("   {}", line));                
        }
        return authorized;

    }
    
}
