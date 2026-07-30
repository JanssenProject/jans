/*
 Copyright (c) 2023, Gluu
 Author: Yuriy Z
 */

import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzdetails.AuthzDetailType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class AuthzDetail implements AuthzDetailType {

    private static final Logger log = LoggerFactory.getLogger(AuthzDetail.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * All validation logic of single authorization detail must take place in this method.
     * If method returns "false" AS returns error to RP. If "true" processing of request goes on.
     *
     * @param scriptContext script context. Authz detail can be taken as "context.getAuthzDetail()".
     * @return whether single authorization detail is valid or not
     */
    @Override
    public boolean validateDetail(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        return context.getAuthzDetail() != null;
    }

    /**
     * Method returns single authorization detail string representation which is shown on authorization page by AS.
     *
     * @param scriptContext script context. Authz detail can be taken as "context.getAuthzDetail()".
     * @return returns single authorization details string representation which is shown on authorization page by AS.
     */
    @Override
    public String getUiRepresentation(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        return context.getAuthzDetail().getJsonObject().optString("ui_representation");
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
