package io.jans.as.server.service.external;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.model.authzen.*;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.authzen.AccessEvaluationType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * External script service for AuthZEN access evaluation.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalAccessEvaluationService extends ExternalScriptService {

    @Inject
    private transient AppConfiguration appConfiguration;

    public ExternalAccessEvaluationService() {
        super(CustomScriptType.ACCESS_EVALUATION);
    }

    public AccessEvaluationResponse externalEvaluate(AccessEvaluationRequest request, ExecutionContext context) {
        final CustomScriptConfiguration script = identifyScript();
        if (script == null) {
            log.debug("Failed to identify script by resource type {}", request.getResource().getType());
            return AccessEvaluationResponse.FALSE;
        }

        context.setScript(script);

        try {
            log.trace("Executing 'externalEvaluate' method, script name: {}, request: {}", script.getName(), request);

            ExternalScriptContext scriptContext = ExternalUpdateTokenContext.of(context);

            AccessEvaluationType evaluationType = (AccessEvaluationType) script.getExternalType();
            AccessEvaluationResponse result = evaluationType.evaluate(request, scriptContext);

            log.trace("Finished 'externalEvaluate' method, script name: {}, result: {} for request: {}, hasWebApplicationException {}",
                    script.getName(), result, request, scriptContext.getWebApplicationException() != null);

            scriptContext.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return AccessEvaluationResponse.FALSE;
    }

    public SearchResponse<Subject> externalSearchSubject(SearchSubjectRequest request, ExecutionContext context) {
        final CustomScriptConfiguration script = identifyScript();
        if (script == null) {
            log.debug("Failed to identify script for search subject");
            return null;
        }

        context.setScript(script);

        try {
            log.trace("Executing 'searchSubject' method, script name: {}, request: {}", script.getName(), request);

            ExternalScriptContext scriptContext = ExternalUpdateTokenContext.of(context);

            AccessEvaluationType evaluationType = (AccessEvaluationType) script.getExternalType();
            SearchResponse<Subject> result = evaluationType.searchSubject(request, scriptContext);

            log.trace("Finished 'searchSubject' method, script name: {}, result: {}", script.getName(), result);

            scriptContext.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return null;
    }

    public SearchResponse<Resource> externalSearchResource(SearchResourceRequest request, ExecutionContext context) {
        final CustomScriptConfiguration script = identifyScript();
        if (script == null) {
            log.debug("Failed to identify script for search resource");
            return null;
        }

        context.setScript(script);

        try {
            log.trace("Executing 'searchResource' method, script name: {}, request: {}", script.getName(), request);

            ExternalScriptContext scriptContext = ExternalUpdateTokenContext.of(context);

            AccessEvaluationType evaluationType = (AccessEvaluationType) script.getExternalType();
            SearchResponse<Resource> result = evaluationType.searchResource(request, scriptContext);

            log.trace("Finished 'searchResource' method, script name: {}, result: {}", script.getName(), result);

            scriptContext.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return null;
    }

    public SearchResponse<Action> externalSearchAction(SearchActionRequest request, ExecutionContext context) {
        final CustomScriptConfiguration script = identifyScript();
        if (script == null) {
            log.debug("Failed to identify script for search action");
            return null;
        }

        context.setScript(script);

        try {
            log.trace("Executing 'searchAction' method, script name: {}, request: {}", script.getName(), request);

            ExternalScriptContext scriptContext = ExternalUpdateTokenContext.of(context);

            AccessEvaluationType evaluationType = (AccessEvaluationType) script.getExternalType();
            SearchResponse<Action> result = evaluationType.searchAction(request, scriptContext);

            log.trace("Finished 'searchAction' method, script name: {}, result: {}", script.getName(), result);

            scriptContext.throwWebApplicationExceptionIfSet();
            return result;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(script.getCustomScript(), e);
        }

        return null;
    }

    private CustomScriptConfiguration identifyScript() {
        final String scriptName = appConfiguration.getAccessEvaluationScriptName();

        CustomScriptConfiguration script = StringUtils.isNotBlank(scriptName) ? getCustomScriptConfigurationByName(scriptName) : null;
        if (script == null) {
            log.trace("Unable to find access_evaluation script by configuration property 'accessEvaluationScriptName' {}", scriptName);
            final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurations();
            if (scripts != null && !scripts.isEmpty()) {
                log.trace("Use first access_evaluation script in database because unable to find script specified by 'accessEvaluationScriptName': {}", scriptName);
                script = scripts.get(0);
            }
        }
        log.debug("Access evaluation with script {}, id {}", script != null ? script.getName() : "", script != null ? script.getInum() : "");
        return script;
    }
}
