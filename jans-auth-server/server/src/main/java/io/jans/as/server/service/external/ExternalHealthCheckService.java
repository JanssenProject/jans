package io.jans.as.server.service.external;

import com.google.common.collect.Sets;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.health.HealthCheckType;
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
public class ExternalHealthCheckService extends ExternalScriptService {

    public ExternalHealthCheckService() {
        super(CustomScriptType.HEALTH_CHECK);
    }

    public String externalHealthCheck(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return null;
        }

        log.trace("Found {} health-check scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final String result = externalHealthCheck(script, context);
            if (StringUtils.isNotBlank(result)) {
                log.debug("'healthCheck' returned result {}, script: {}", result, script.getName());
                return result;
            }
        }

        return null;
    }

    private String externalHealthCheck(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'healthCheck' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            HealthCheckType script = (HealthCheckType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);

            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            final String result = script.healthCheck(scriptContext);

            log.trace("Finished external 'healthCheck' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return null;
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
