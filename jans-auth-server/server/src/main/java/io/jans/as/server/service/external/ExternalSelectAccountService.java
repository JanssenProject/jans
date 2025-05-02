package io.jans.as.server.service.external;

import com.google.common.collect.Sets;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.selectaccount.SelectAccountType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * @author Yuriy Z
 */

@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalSelectAccountService extends ExternalScriptService {

    public ExternalSelectAccountService() {
        super(CustomScriptType.SELECT_ACCOUNT);
    }

    public String externalGetAccountDisplayName(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return "";
        }

        log.trace("Found {} 'select-account' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final String name = externalGetAccountDisplayName(script, context);
            if (StringUtils.isNotBlank(name)) {
                log.debug("DisplayName '{}' provided by external script: {}", name, script.getName());
                return name;
            }
        }

        return "";
    }

    public String externalGetAccountDisplayName(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'externalGetAccountDisplayName' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            SelectAccountType script = (SelectAccountType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final String result = script.getAccountDisplayName(context);

            log.trace("Finished external 'externalGetAccountDisplayName' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return "";
        }
    }


    public boolean externalPrepare(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return true;
        }

        log.trace("Found {} 'selectaccount' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final boolean result = externalPrepare(script, context);
            if (!result) {
                log.debug("'prepare' return false, script: {}", script.getName());
                return false;
            }
        }

        return true;
    }

    public boolean externalPrepare(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'externalPrepare' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            SelectAccountType script = (SelectAccountType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.prepare(context);

            log.trace("Finished external 'externalPrepare' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public boolean externalOnSelect(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return true;
        }

        log.trace("Found {} select-account scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final boolean result = externalOnSelect(script, context);
            if (!result) {
                log.debug("onSelect return false, script: {}", script.getName());
                return false;
            }
        }

        return true;
    }

    public boolean externalOnSelect(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'externalOnSelect' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            SelectAccountType script = (SelectAccountType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final boolean result = script.onSelect(context);

            log.trace("Finished external 'externalOnSelect' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public String externalGetSelectAccountPage(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return "";
        }

        log.trace("Found {} select-account scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final String page = externalGetSelectAccountPage(script, context);
            if (StringUtils.isNotBlank(page)) {
                log.debug("SelectAccount page {} provided by external script: {}", page, script.getName());
                return page;
            }
        }

        return "";
    }

    public String externalGetSelectAccountPage(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'externalGetSelectAccountPage' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            SelectAccountType script = (SelectAccountType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);
            final String result = script.getSelectAccountPage(context);

            log.trace("Finished external 'externalGetSelectAccountPage' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return "";
        }
    }

    private Set<CustomScriptConfiguration> getScriptsToExecute() {
        Set<CustomScriptConfiguration> result = Sets.newHashSet();
        if (this.customScriptConfigurations == null) {
            return result;
        }

        result.addAll(customScriptConfigurations);
        return result;
    }
}
