import java.util.HashMap;
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
import io.jans.as.server.service.external.context.ExternalPostAuthnContext;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import org.json.JSONObject;

/**
 * Sample post-authentication script demonstrating Cedarling authorization.
 *
 * <h3>Migration guide (v16.0 → v17.0)</h3>
 *
 * <p><b>Before (v16.0) – single {@code authorize} method:</b></p>
 * <pre>{@code
 *   Map<String, String> tokens = Map.of(
 *       "access_token", accessJwt,
 *       "id_token",     idJwt
 *   );
 *   AuthorizeResult result = adapter.authorize(tokens, action, resource, context);
 * }</pre>
 *
 * <p><b>After (v17.0) – use {@code authorizeMultiIssuer} with JWT tokens:</b></p>
 * <pre>{@code
 *   Map<String, String> tokens = Map.of(
 *       "Jans::Access_Token", accessJwt,
 *       "Jans::Id_Token",     idJwt
 *   );
 *   MultiIssuerAuthorizeResult result =
 *       adapter.authorizeMultiIssuer(tokens, action, resource, context);
 * }</pre>
 *
 * <p>The key change is using Cedar entity type names as keys
 * (e.g. {@code "Jans::Access_Token"}) instead of short token names.</p>
 *
 * <p>For {@code authorizeMultiIssuer}, the bootstrap policy store schema must support
 * multi-issuer evaluation (including {@code context.tokens}, when policies reference it).
 * A reference store is {@code jans-cedarling/test_files/policy-store-multi-issuer-test.yaml}.</p>
 *
 * <p>If you have pre-validated / unsigned entity data instead of JWTs, use
 * {@code authorizeUnsigned} — no UniFFI imports needed:</p>
 * <pre>{@code
 *   AuthorizeResult result =
 *       adapter.authorizeUnsigned(principalJsonString, action, resource, context);
 * }</pre>
 */
public class PostAuthn implements PostAuthnType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
    CedarlingAdapter cedarlingAdapter = null;
    String action = null;
    String resourceStr = null;
    String contextStr = null;

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
        if (!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property BOOTSTRAP_JSON_PATH is not specified.");
            return true;
        }
        log.info("Initialize Cedarling...");

        String bootstrapFilePath = configurationAttributes.get("BOOTSTRAP_JSON_PATH").getValue2();
        String actionFilePath = configurationAttributes.get("ACTION_FILE_PATH").getValue2();
        String resourceFilePath = configurationAttributes.get("RESOURCE_FILE_PATH").getValue2();
        String contextFilePath = configurationAttributes.get("CONTEXT_FILE_PATH").getValue2();

        try {
            String bootstrapJson = readFile(bootstrapFilePath);
            action = readFile(actionFilePath);
            resourceStr = readFile(resourceFilePath);
            contextStr = readFile(contextFilePath);
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJson);
        } catch (CedarlingException e) {
            log.error("Unable to initialize Cedarling: " + e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling: " + e.getMessage());
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

    /**
     * Demonstrates JWT-based authorization using {@code authorizeMultiIssuer}.
     *
     * <p>This replaces the old {@code authorize(Map&lt;String,String&gt;, ...)}
     * method. Pass raw JWT strings in a {@code Map<String, String>} keyed by token
     * mapping names (e.g. {@code "Jans::Access_Token"}) as required by your policy store.</p>
     *
     * <p><b>Post-authn return value:</b> per {@link PostAuthnType#forceAuthorization(Object)},
     * returning {@code true} sends the user through the OAuth authorization step again;
     * {@code false} does not force that redirect. This sample maps Cedarling {@code deny}
     * to {@code true} (force authorization) and {@code allow} to {@code false}. Adjust for
     * your product policy; you may need tokens from the session or grant store rather than
     * request parameters.</p>
     */
    @Override
    public boolean forceAuthorization(Object context) {
        log.info("Inside forceAuthorization method...");
        if (cedarlingAdapter == null) {
            log.error("Cedarling is not initialized; skipping Cedarling authorization check.");
            return false;
        }

        ExternalPostAuthnContext postAuthnContext = (ExternalPostAuthnContext) context;
        try {
            JSONObject resourceJson = new JSONObject(resourceStr);
            JSONObject contextJson = new JSONObject(contextStr);

            // Illustrative only: authorize requests typically do not pass JWTs as query params.
            // In production, obtain access/id tokens from session or other server-side state.
            String accessTokenJwt = postAuthnContext.getHttpRequest().getParameter("access_token");
            String idTokenJwt = postAuthnContext.getHttpRequest().getParameter("id_token");

            Map<String, String> tokens = new HashMap<>();
            if (accessTokenJwt != null && !accessTokenJwt.isEmpty()) {
                tokens.put("Jans::Access_Token", accessTokenJwt);
            }
            if (idTokenJwt != null && !idTokenJwt.isEmpty()) {
                tokens.put("Jans::Id_Token", idTokenJwt);
            }
            if (tokens.isEmpty()) {
                log.warn("No JWTs available for Cedarling authorizeMultiIssuer; forcing authorization step.");
                return true;
            }

            MultiIssuerAuthorizeResult result =
                    cedarlingAdapter.authorizeMultiIssuer(tokens, action, resourceJson, contextJson);

            boolean cedarlingAllowed = result.getDecision();
            log.info("Cedarling Authz Response Decision: {}", cedarlingAllowed);
            // true = force OAuth authorization redirect; false = do not force
            return !cedarlingAllowed;
        } catch (AuthorizeException | EntityException e) {
            log.error("Error in Cedarling Authz: " + e.getMessage());
            return true;
        }
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