package io.jans.as.server.service.external;

import com.google.common.collect.Sets;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.createuser.CreateUserType;
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
public class ExternalCreateUserService extends ExternalScriptService {

    public ExternalCreateUserService() {
        super(CustomScriptType.CREATE_USER);
    }

    public boolean externalCreate(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return true;
        }

        log.trace("Found {} create-user scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final boolean result = externalCreate(script, context);
            if (!result) {
                log.debug("'createUser' returned false, script: {}", script.getName());
                return false;
            }
        }

        return true;
    }

    public boolean externalCreate(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'createUser' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            CreateUserType script = (CreateUserType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);

            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            final boolean result = script.createUser(scriptContext);

            log.trace("Finished external 'createUser' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public String externalGetCreateUserPage(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return "";
        }

        log.trace("Found {} create-user scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final String page = externalGetCreateUserPage(script, context);
            if (StringUtils.isNotBlank(page)) {
                log.debug("CreateUser page {} provided by external script: {}", page, script.getName());
                return page;
            }
        }

        return "";
    }

    public String externalGetCreateUserPage(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'externalGetCreateUserPage' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            CreateUserType script = (CreateUserType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);

            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            final String result = script.getCreateUserPage(scriptContext);

            log.trace("Finished external 'externalGetCreateUserPage' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
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

        log.trace("Found {} 'createuser' scripts.", scripts.size());

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

            CreateUserType script = (CreateUserType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);

            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            final boolean result = script.prepare(scriptContext);

            log.trace("Finished external 'externalPrepare' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
            return result;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(scriptConfiguration.getCustomScript(), ex);
            return false;
        }
    }

    public String externalBuildPostAuthorizeUrl(ExecutionContext context) {
        final Set<CustomScriptConfiguration> scripts = getScriptsToExecute();
        if (scripts.isEmpty()) {
            return "";
        }

        log.trace("Found {} 'createuser' scripts.", scripts.size());

        for (CustomScriptConfiguration script : scripts) {
            final String result = externalBuildPostAuthorizeUrl(script, context);
            if (StringUtils.isNotBlank(result)) {
                log.debug("'buildPostAuthorizeUrl' return {}, script: {}", result, script.getName());
                return result;
            }
        }

        return "";
    }

    public String externalBuildPostAuthorizeUrl(CustomScriptConfiguration scriptConfiguration, ExecutionContext context) {
        try {
            log.trace("Executing external 'buildPostAuthorizeUrl' method, script name: {}, context: {}", scriptConfiguration.getName(), context);

            CreateUserType script = (CreateUserType) scriptConfiguration.getExternalType();
            context.setScript(scriptConfiguration);

            final ExternalScriptContext scriptContext = new ExternalScriptContext(context);
            final String result = script.buildPostAuthorizeUrl(scriptContext);

            log.trace("Finished external 'buildPostAuthorizeUrl' method, script name: {}, context: {}, result: {}", scriptConfiguration.getName(), context, result);
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
