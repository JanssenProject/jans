## bootstrap.json

```declarative
{
"CEDARLING_APPLICATION_NAME": "Cedarling-Test-In-Custom-Script",
"CEDARLING_POLICY_STORE_LOCAL_FN": "./custom/static/update_token_script.cjar",
"CEDARLING_LOG_LEVEL": "DEBUG",
"CEDARLING_LOG_TYPE": "std_out",
"CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
"===": [{"var": "Jans::Workload"}, "ALLOW"]
}
}
```

## sample_cedarling_update_token.java

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.UpdateTokenType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniffi.cedarling_uniffi.*;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import java.util.Map;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.model.common.AccessToken;
import org.json.JSONObject;
import java.util.List;
import java.util.ArrayList;
import io.jans.as.model.common.GrantType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.common.collect.Sets;


public class UpdateTokenCedarling implements UpdateTokenType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
    CedarlingAdapter cedarlingAdapter = null;


    public boolean init(Map < String, SimpleCustomProperty > configurationAttributes) {
        log.info("UpdateTokenCedarling initialized!!!");
        // Check if bootstrap JSON file path is configured
        if (!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property BOOTSTRAP_JSON_PATH is not specified.");
            return false;
        }
        log.info("Initialize Cedarling...");

        // Get bootstrap file path
        String bootstrapFilePath = configurationAttributes.get("BOOTSTRAP_JSON_PATH").getValue2();

        String bootstrapJson = null;
        try {
            // Read bootstrap JSON from file
            bootstrapJson = readFile(bootstrapFilePath);
            // Initialize Cedarling adapter
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJson);
        } catch (CedarlingException e) {
            log.error("Unable to initialize Cedarling: {}", e.getMessage(), e);
            cedarlingAdapter = null;
            return false;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling: {}", e.getMessage(), e);
            cedarlingAdapter = null;
            return false;
        }
        log.info("Cedarling Initialization successful...");

        return true;
    }

    public boolean init(CustomScript customScript, Map < String, SimpleCustomProperty > configurationAttributes) {
        log.info("UpdateTokenCedarling initialized!!!");
        // Check if bootstrap JSON file path is configured
        if (!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property BOOTSTRAP_JSON_PATH is not specified.");
            return false;
        }
        log.info("Initialize Cedarling...");

        // Get bootstrap file path
        String bootstrapFilePath = configurationAttributes.get("BOOTSTRAP_JSON_PATH").getValue2();

        String bootstrapJson = null;
        try {
            // Read bootstrap JSON from file
            bootstrapJson = readFile(bootstrapFilePath);
            // Initialize Cedarling adapter
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJson);
        } catch (CedarlingException e) {
            log.error("Unable to initialize Cedarling: {}", e.getMessage(), e);
            cedarlingAdapter = null;
            return false;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling: {}", e.getMessage(), e);
            cedarlingAdapter = null;
            return false;
        }
        log.info("Cedarling Initialization successful...");

        return true;
    }

    public boolean destroy(Map < String, SimpleCustomProperty > configurationAttributes) {
        return true;
    }

    public int getApiVersion() {
        return 1;
    }

    @Override
    public boolean modifyIdToken(Object jwr, Object tokenContext) {
        return false;
    }

    @Override
    public boolean modifyRefreshToken(Object refreshToken, Object tokenContext) {
        return false;
    }

    @Override
    public boolean modifyAccessToken(Object accessTokenObj, Object contextObj) {
        try {
            AccessToken accessToken = (AccessToken) accessTokenObj;
            ExternalUpdateTokenContext context = (ExternalUpdateTokenContext) contextObj;

            // Collect client grant types
            List < String > grantTypes = new ArrayList < > ();
            for (GrantType gt: context.getClient().getGrantTypes()) {
                grantTypes.add(gt.getValue());
            }

            // Build principal and resource entities
            JSONObject principalJson = buildPrincipalJson(context.getClient().getClientId(), grantTypes);
            JSONObject resourceJson = buildResourceJson(grantTypes);

            // Convert principal JSON to EntityData
            List < EntityData > principalEntities =
                    List.of(EntityData.Companion.fromJson(principalJson.toString()));

            // Empty context (placeholder for future extension)
            JSONObject contextJson = new JSONObject("{}");

            // Call Cedarling authorization
            AuthorizeResult result = cedarlingAdapter.authorizeUnsigned(
                    principalEntities,
                    "Jans::Action::\"Execute\"",
                    resourceJson,
                    contextJson
            );

            log.info("Cedarling Authz Response Decision: {}", result.getDecision());

            // If authorized → overwrite scopes
            if (result.getDecision()) {
                context.overwriteAccessTokenScopes(accessToken, Sets.newHashSet("openid", "profile"));
                return true;
            }

        } catch (AuthorizeException | EntityException e) {
            log.error("Error in Cedarling Authz", e);
            return false;
        }

        return false;
    }

    /** Helper: Build Principal JSON
     * {
     *    "cedar_entity_mapping": {
     *        "entity_type": "Jans::Workload",
     *        "id": "wl_id"
     *    },
     *    "client_id": "xxxxxxxx-xxxxx-xxxx-xxxxxxxx",
     *    "grantTypes": ["authorization_code", "refresh_token"]
     *}
     * */
    private JSONObject buildPrincipalJson(String clientId, List < String > grantTypes) {
        JSONObject json = new JSONObject();
        json.put("cedar_entity_mapping", new JSONObject()
                .put("entity_type", "Jans::Workload")
                .put("id", "wl_id"));
        json.put("client_id", clientId);
        json.put("grantTypes", grantTypes);

        log.debug("Principal JSON: {}", json);
        return json;
    }

    /** Helper: Build Resource JSON
     *
     * {
     *    "cedar_entity_mapping": {
     *        "entity_type": "Jans::Application",
     *        "id": "app_id"
     *    },
     *    "grantTypes": ["authorization_code", "client_credentials"]
     *}
     * */
    private JSONObject buildResourceJson(List < String > grantTypes) {
        JSONObject json = new JSONObject();
        json.put("cedar_entity_mapping", new JSONObject()
                .put("entity_type", "Jans::Application")
                .put("id", "app_id"));
        json.put("grantTypes", grantTypes);

        log.debug("Resource JSON: {}", json);
        return json;
    }

    @Override
    public int getRefreshTokenLifetimeInSeconds(Object tokenContext) {
        return 0;
    }

    @Override
    public int getIdTokenLifetimeInSeconds(Object context) {
        return 0;
    }

    @Override
    public int getAccessTokenLifetimeInSeconds(Object context) {
        return 0;
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
```
