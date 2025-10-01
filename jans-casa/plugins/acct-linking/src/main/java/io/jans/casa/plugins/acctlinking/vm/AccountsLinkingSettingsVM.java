package io.jans.casa.plugins.acctlinking.vm;

import io.jans.casa.plugins.acctlinking.AccountsLinkingService;
import io.jans.inbound.Provider;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;

public class AccountsLinkingSettingsVM {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private String error;
    
    private Map<String, Provider> providers = Collections.emptyMap();
    
    public Map<String, Provider> getProviders() {
        return providers;
    }
    
    public String getError() {
        return error;
    }
        
    @Init
    public void init() {
        
        try {
            logger.info("Refreshing list of identity providers");
            providers = AccountsLinkingService.getInstance().getProviders(false);
        } catch (Exception e) {
            providers = null;
            error = e.getMessage();
            logger.error(error, e);
        }
        
    }
    
}
