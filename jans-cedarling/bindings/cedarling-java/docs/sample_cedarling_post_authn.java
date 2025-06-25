import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.service.custom.script.CustomScriptManager;
import uniffi.cedarling_uniffi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

public class PostAuthn implements PostAuthnType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
    CedarlingAdapter cedarlingAdapter = null;
    String action = null;
    String resourceStr = null;
    String contextStr = null;
    String principalsStr = null;

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        if(!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property bootstrap_file_path is not specified.");
            return true;
        }
        log.info("Initialize Cedarling...");

        // Read input files for authorization
        String bootstrapFilePath = configurationAttributes.get("BOOTSTRAP_JSON_PATH").getValue2();
        String actionFilePath = configurationAttributes.get("ACTION_FILE_PATH").getValue2();
        String resourceFilePath = configurationAttributes.get("RESOURCE_FILE_PATH").getValue2();
        String contextFilePath = configurationAttributes.get("CONTEXT_FILE_PATH").getValue2();
        String principalsFilePath = configurationAttributes.get("PRINCIPALS_FILE_PATH").getValue2();

        String bootstrapJson = null;
        try {
            bootstrapJson = readFile(bootstrapFilePath);
            action = readFile(actionFilePath);
            resourceStr = readFile(resourceFilePath);
            contextStr = readFile(contextFilePath);
            principalsStr = readFile(principalsFilePath);
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJson);
        } catch (CedarlingException e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        }
            log.info("Cedarling Initialization successful...");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Destroying...");
        log.info("Post Authentication. Destroyed.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean forceReAuthentication(Object context) {
        return false;
    }

    @Override
    public boolean forceAuthorization(Object context) {
        log.info("Inside forceAuthorization method...");
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        try {
            List<EntityData> principalsJson = List.of(EntityData.Companion.fromJson(principalsStr));
            JSONObject resourceJson = new JSONObject(resourceStr);
            JSONObject contextJson = new JSONObject(contextStr);

            AuthorizeResult result = cedarlingAdapter.authorizeUnsigned(principalsJson, action, resourceJson, contextJson);
            cedarlingAdapter.close();
            log.info("Cedarling Authz Response Decision: " + result.getDecision());
            //logic to to use the Cedarling authorization decision ...
        } catch(AuthorizeException | EntityException e) {
            log.error("Error in Cedarling Authz: " + e.getMessage());
            return false;
        }
        return false;
    }

    public String readFile(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath();
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}