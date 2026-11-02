import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
import io.jans.model.authzen.AccessEvaluationResponseContext;
import io.jans.model.authzen.Resource;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzen.AccessEvaluationType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
public class AccessEvaluation implements AccessEvaluationType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object scriptContext) {

        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        // 1. access http request via context.getHttpRequest()
        // 2. access all access evaluation specific data directly with 'request', e.g. request.getSubject()

        // 3. perform custom validation if needed
        validateResource(request.getResource());

        // typically some internal validation must be performed here
        // request data alone must not be trusted, it's just sample to demo script with endpoint
        if ("super_admin".equalsIgnoreCase(request.getSubject().getType())) {
            final ObjectNode reasonAdmin = objectMapper.createObjectNode();
            reasonAdmin.put("reason", "super_admin");

            final AccessEvaluationResponseContext responseContext = new AccessEvaluationResponseContext();
            responseContext.setId(UUID.randomUUID().toString());
            responseContext.setReasonAdmin(reasonAdmin);

            return new AccessEvaluationResponse(true, responseContext);
        }
        return AccessEvaluationResponse.FALSE;
    }

    private void validateResource(Resource resource) {
        // sample for custom validation
        if (resource.getType().equalsIgnoreCase("file")) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\n" +
                            "  \"error\": \"invalid_resource_type\",\n" +
                            "  \"error_description\": \"Resource type 'file' is not allowed.\"\n" +
                            "}")
                    .build());
        }
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AccessEvaluation Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AccessEvaluation Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed AccessEvaluation Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
