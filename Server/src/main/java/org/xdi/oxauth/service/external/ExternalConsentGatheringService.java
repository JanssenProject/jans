package org.xdi.oxauth.service.external;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.authz.ConsentGatheringType;
import org.gluu.service.LookupService;
import org.gluu.service.custom.script.ExternalScriptService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.service.external.context.ConsentGatheringContext;

/**
 * @author Yuriy Movchan Date: 10/30/2017
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalConsentGatheringService extends ExternalScriptService {

	private static final long serialVersionUID = 1741073794567832914L;

	@Inject
    private Logger log;

    @Inject
    private LookupService lookupService;

    protected Map<String, CustomScriptConfiguration> scriptInumMap;

    public ExternalConsentGatheringService() {
        super(CustomScriptType.CONSENT_GATHERING);
    }

    @Override
    protected void reloadExternal() {
        this.scriptInumMap = buildExternalConfigurationsInumMap(this.customScriptConfigurations);
    }

    private Map<String, CustomScriptConfiguration> buildExternalConfigurationsInumMap(List<CustomScriptConfiguration> customScriptConfigurations) {
        Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());

        for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
            reloadedExternalConfigurations.put(customScriptConfiguration.getInum(), customScriptConfiguration);
        }

        return reloadedExternalConfigurations;
    }

    public CustomScriptConfiguration getScriptByDn(String scriptDn) {
        String consentScriptInum = lookupService.getInumFromDn(scriptDn);

        return getScriptByInum(consentScriptInum);
    }

    public CustomScriptConfiguration getScriptByInum(String inum) {
        if (StringHelper.isEmpty(inum)) {
            return null;
        }

        return this.scriptInumMap.get(inum);
    }

    private ConsentGatheringType consentScript(CustomScriptConfiguration script) {
        return (ConsentGatheringType) script.getExternalType();
    }

    public boolean authorize(CustomScriptConfiguration script, int step, ConsentGatheringContext context) {
        try {
            log.debug("Executing python 'authorize' method, script: " + script.getName());
            boolean result = consentScript(script).authorize(step, context);
            log.debug("python 'authorize' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'authorize' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return false;
        }
    }

    public int getNextStep(CustomScriptConfiguration script, int step, ConsentGatheringContext context) {
        try {
            log.debug("Executing python 'getNextStep' method, script: " + script.getName());
            int result = consentScript(script).getNextStep(step, context);
            log.debug("python 'getNextStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getNextStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return -1;
        }
    }

    public boolean prepareForStep(CustomScriptConfiguration script, int step, ConsentGatheringContext context) {
        try {
            log.debug("Executing python 'prepareForStep' method, script: " + script.getName());
            boolean result = consentScript(script).prepareForStep(step, context);
            log.debug("python 'prepareForStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'prepareForStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return false;
        }
    }

    public int getStepsCount(CustomScriptConfiguration script, ConsentGatheringContext context) {
        try {
            log.debug("Executing python 'getStepsCount' method, script: " + script.getName());
            int result = consentScript(script).getStepsCount(context);
            log.debug("python 'getStepsCount' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getStepsCount' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return -1;
        }
    }

    public String getPageForStep(CustomScriptConfiguration script, int step, ConsentGatheringContext context) {
        try {
            log.debug("Executing python 'getPageForStep' method, script: " + script.getName());
            String result = consentScript(script).getPageForStep(step, context);
            log.debug("python 'getPageForStep' result: " + result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to execute python 'getPageForStep' method, script: " + script.getName() + ", message: " + ex.getMessage(), ex);
            saveScriptError(script.getCustomScript(), ex);
            return "";
        }
    }
}
