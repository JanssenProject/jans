package io.jans.casa.plugins.acctlinking;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountsLinkingPlugin extends Plugin {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    public AccountsLinkingPlugin(PluginWrapper wrapper) throws Exception {
        super(wrapper);
        // "initialize" service
        AccountsLinkingService.getInstance(wrapper.getPluginId());
    }

    @Override
    public void start() {
    }

    @Override
    public void delete() {
    }

}
