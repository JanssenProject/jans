## bootstrap.json

```declarative
{
    "CEDARLING_APPLICATION_NAME": "Cedarling-Test-In-Custom-Script",
    "CEDARLING_AUDIT_HEALTH_INTERVAL": 0,
    "CEDARLING_AUDIT_TELEMETRY_INTERVAL": 0,
    "CEDARLING_DYNAMIC_CONFIGURATION": "disabled",
    "CEDARLING_ID_TOKEN_TRUST_MODE": "never",
    "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
    "HS256",
    "RS256"
    ],
    "CEDARLING_JWT_SIG_VALIDATION": "disabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
    "CEDARLING_LISTEN_SSE": "disabled",
    "CEDARLING_LOCAL_JWKS": null,
    "CEDARLING_LOCK": "disabled",
    "CEDARLING_LOCK_SSA_JWT": null,
    "CEDARLING_LOG_LEVEL": "DEBUG",
    "CEDARLING_LOG_TYPE": "std_out",
    "CEDARLING_POLICY_STORE_ID": "cdeb4b635459898300a5893589a76b726e202dcb5a86",
    "CEDARLING_POLICY_STORE_URI": "./custom/static/policy-store.json",
    "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
    "===": [
            {
            "var": "Jans::Workload"
            },
            "ALLOW"
        ]
    },
    "CEDARLING_USER_AUTHZ": "enabled",
    "CEDARLING_WORKLOAD_AUTHZ": "enabled",
    "id": "7a962b6e-aa45-4418-a94a-ee382d20a723"
}
```

## policy-store.json

```declarative
{
    "cedar_version": "4.4.0",
    "policy_stores": {
        "3cf98caf8e7fdb289c922ba9514118dcba716ce426ae": {
        "name": "admin_ui_store",
        "description": "Admin UI Policy Store\t",
        "policies": {
        "7c52efd895db799901d8590a2c5e3a76539d4a6de514": {
            "description": "Authorization policy",
            "creation_date": "2025-06-09T12:32:26.114299",
            "policy_content": "QGlkKCJBdXRob3JpemF0aW9uIHBvbGljeSIpCnBlcm1pdCAoCiAgcHJpbmNpcGFsIGlzIEphbnM6Oldvcmtsb2FkLAogIGFjdGlvbiA9PSBKYW5zOjpBY3Rpb246OiJFeGVjdXRlIiwKICByZXNvdXJjZSBpcyBKYW5zOjpBcHBsaWNhdGlvbgopCndoZW4gewogIHByaW5jaXBhbC5ncmFudFR5cGVzLmNvbnRhaW5zKCJhdXRob3JpemF0aW9uX2NvZGUiKQp9Ow=="
            }
        },
        "trusted_issuers": {},
        "schema": "eyJKYW5zIjp7ImNvbW1vblR5cGVzIjp7IkNvbnRleHQiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY3VycmVudF90aW1lIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsInJlcXVpcmVkIjpmYWxzZSwibmFtZSI6IkxvbmcifSwiZGV2aWNlX2hlYWx0aCI6eyJ0eXBlIjoiU2V0IiwicmVxdWlyZWQiOmZhbHNlLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX0sImZyYXVkX2luZGljYXRvcnMiOnsidHlwZSI6IlNldCIsInJlcXVpcmVkIjpmYWxzZSwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19LCJnZW9sb2NhdGlvbiI6eyJ0eXBlIjoiU2V0IiwicmVxdWlyZWQiOmZhbHNlLCJlbGVtZW50Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX0sIm5ldHdvcmsiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwicmVxdWlyZWQiOmZhbHNlLCJuYW1lIjoiU3RyaW5nIn0sIm5ldHdvcmtfdHlwZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJTdHJpbmcifSwib3BlcmF0aW5nX3N5c3RlbSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJTdHJpbmcifSwidXNlcl9hZ2VudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJTdHJpbmcifX19LCJlbWFpbF9hZGRyZXNzIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImRvbWFpbiI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInVpZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19fSwiVXJsIjp7InR5cGUiOiJTdHJpbmcifX0sImVudGl0eVR5cGVzIjp7IkFwcGxpY2F0aW9uIjp7InNoYXBlIjp7InR5cGUiOiJSZWNvcmQiLCJhdHRyaWJ1dGVzIjp7ImdyYW50VHlwZXMiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19fSwiUm9sZSI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6e319fSwiVHJ1c3RlZElzc3VlciI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJpc3N1ZXJfZW50aXR5X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJVcmwifX19fSwiVXNlciI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJlbWFpbCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJlbWFpbF9hZGRyZXNzIn0sInJvbGUiOnsidHlwZSI6IlNldCIsImVsZW1lbnQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fSwic3ViIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsInJlcXVpcmVkIjpmYWxzZSwibmFtZSI6IlN0cmluZyJ9fX0sIm1lbWJlck9mVHlwZXMiOlsiUm9sZSJdfSwiV29ya2xvYWQiOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY2xpZW50X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwiaXNzIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsInJlcXVpcmVkIjpmYWxzZSwibmFtZSI6IlRydXN0ZWRJc3N1ZXIifSwibmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJTdHJpbmcifSwicnBfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwicmVxdWlyZWQiOmZhbHNlLCJuYW1lIjoiU3RyaW5nIn0sInNwaWZmZV9pZCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJyZXF1aXJlZCI6ZmFsc2UsIm5hbWUiOiJTdHJpbmcifSwiZ3JhbnRUeXBlcyI6eyJ0eXBlIjoiU2V0IiwiZWxlbWVudCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19fX19fSwiYWN0aW9ucyI6eyJFeGVjdXRlIjp7ImFwcGxpZXNUbyI6eyJwcmluY2lwYWxUeXBlcyI6WyJXb3JrbG9hZCJdLCJyZXNvdXJjZVR5cGVzIjpbIkFwcGxpY2F0aW9uIl0sImNvbnRleHQiOnsidHlwZSI6IkNvbnRleHQifX19fX19"
        }
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
            log.error("Initialization. Property bootstrap_file_path is not specified.");
            return true;
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
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        }
        log.info("Cedarling Initialization successful...");

        return true;
    }

    public boolean init(CustomScript customScript, Map < String, SimpleCustomProperty > configurationAttributes) {
        log.info("UpdateTokenCedarling initialized!!!");
        // Check if bootstrap JSON file path is configured
        if (!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property bootstrap_file_path is not specified.");
            return true;
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
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
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
