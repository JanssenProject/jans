/* Copyright (c) 2025, Gluu
 */

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.cookie.CookieType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class Cookie implements CookieType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public String modifyCookieHeader(String cookieName, String cookieHeader) {

        // append "Secure" attribute to session_id cookie if it's not present yet
        if (cookieName.equals("session_id") && !cookieHeader.contains("Secure")) {
            cookieHeader = cookieHeader + "; Secure";
        }

        return cookieHeader;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
