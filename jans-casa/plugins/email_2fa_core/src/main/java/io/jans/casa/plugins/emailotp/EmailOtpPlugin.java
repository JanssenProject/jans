package io.jans.casa.plugins.emailotp;

import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailOtpPlugin extends Plugin{

    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(EmailOtpPlugin.class);  

    public EmailOtpPlugin(PluginWrapper wrapper) {
        super(wrapper);

        EmailOtpService emailOTPService = EmailOtpService.getInstance();
        emailOTPService.init();
    }

    /**
     * This method is called by the application when the plugin is started.
     * See {@link PluginManager#startPlugin(String)}.
     */
    @Override
    public void start() {
        // Do nothing.
    }

    /**
     * This method is called by the application when the plugin is stopped.
     * See {@link PluginManager#stopPlugin(String)}.
     */
    @Override
    public void stop() {
        // Do nothing.
    }

    /**
     * This method is called by the application when the plugin is deleted.
     * See {@link PluginManager#deletePlugin(String)}.
     */
    @Override
    public void delete() {
        // Do nothing.
    }

}
