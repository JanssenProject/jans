/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.server.uma.authorization.UmaGatherContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.uma.UmaClaimsGatheringType;
import io.jans.service.LookupService;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.service.custom.script.ExternalScriptService;
import io.jans.service.custom.script.ExternalTypeCreator;
import io.jans.util.StringHelper;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 06/18/2017.
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalUmaClaimsGatheringService extends ExternalScriptService {

    @Inject
    private LookupService lookupService;
    @Inject
    private CustomScriptManager scriptManager;
    @Inject
    private ExternalTypeCreator externalTypeCreator;

    protected Map<String, CustomScriptConfiguration> scriptInumMap;

    public ExternalUmaClaimsGatheringService() {
        super(CustomScriptType.UMA_CLAIMS_GATHERING);
    }

    @Override
    protected void reloadExternal() {
        this.scriptInumMap = buildExternalConfigurationsInumMap(this.customScriptConfigurations);
    }

    public CustomScriptConfiguration determineScript(String[] scriptNames) {
        log.trace("Trying to determine claims-gathering script, scriptNames: {} ...", Arrays.toString(scriptNames));

        List<CustomScriptConfiguration> scripts = new ArrayList<CustomScriptConfiguration>();

        for (String scriptName : scriptNames) {
            CustomScriptConfiguration script = getCustomScriptConfigurationByName(scriptName);
            if (script != null) {
                scripts.add(script);
            } else {
                log.error("Failed to load claims-gathering script with name: {}", scriptName);
            }
        }

        if (scripts.isEmpty()) {
            return null;
        }

        CustomScriptConfiguration highestPriority = Collections.max(scripts, new Comparator<CustomScriptConfiguration>() {
            @Override
            public int compare(CustomScriptConfiguration o1, CustomScriptConfiguration o2) {
                return Integer.compare(o1.getLevel(), o2.getLevel());
            }
        });
        log.trace("Determined claims-gathering script successfully. Name: {}, inum: {}", highestPriority.getName(), highestPriority.getInum());
        return highestPriority;
    }

    private Map<String, CustomScriptConfiguration> buildExternalConfigurationsInumMap(List<CustomScriptConfiguration> customScriptConfigurations) {
        Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());

        for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
            reloadedExternalConfigurations.put(customScriptConfiguration.getInum(), customScriptConfiguration);
        }

        return reloadedExternalConfigurations;
    }

    public CustomScriptConfiguration getScriptByDn(String scriptDn) {
        String authorizationPolicyInum = lookupService.getInumFromDn(scriptDn);

        return getScriptByInum(authorizationPolicyInum);
    }

    public CustomScriptConfiguration getScriptByInum(String inum) {
        if (StringHelper.isEmpty(inum)) {
            return null;
        }

        return this.scriptInumMap.get(inum);
    }

    private UmaClaimsGatheringType gatherScript(CustomScriptConfiguration script) {
        return ExternalUmaRptPolicyService.HOTSWAP_UMA_SCRIPT ? (UmaClaimsGatheringType) ExternalUmaRptPolicyService.hotswap(externalTypeCreator, script, false) : (UmaClaimsGatheringType) script.getExternalType();
    }

    public boolean gather(CustomScriptConfiguration script, int step, UmaGatherContext context) {
        try {
            log.debug("Executing python 'gather' method, script: " + script.getName());
            boolean result = gatherScript(script).gather(step, context);
            log.debug("python 'gather' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'gather' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return false;
        }
    }

    public int getNextStep(CustomScriptConfiguration script, int step, UmaGatherContext context) {
        try {
            log.debug("Executing python 'getNextStep' method, script: " + script.getName());
            int result = gatherScript(script).getNextStep(step, context);
            log.debug("python 'getNextStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getNextStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return -1;
        }
    }

    public boolean prepareForStep(CustomScriptConfiguration script, int step, UmaGatherContext context) {
        try {
            log.debug("Executing python 'prepareForStep' method, script: " + script.getName());
            boolean result = gatherScript(script).prepareForStep(step, context);
            log.debug("python 'prepareForStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'prepareForStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return false;
        }
    }

    public int getStepsCount(CustomScriptConfiguration script, UmaGatherContext context) {
        try {
            log.debug("Executing python 'getStepsCount' method, script: " + script.getName());
            int result = gatherScript(script).getStepsCount(context);
            log.debug("python 'getStepsCount' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getStepsCount' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return -1;
        }
    }

    public String getPageForStep(CustomScriptConfiguration script, int step, UmaGatherContext context) {
        try {
            log.debug("Executing python 'getPageForStep' method, script: " + script.getName());
            String result = gatherScript(script).getPageForStep(step, context);
            log.debug("python 'getPageForStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getPageForStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return "";
        }
    }
}
